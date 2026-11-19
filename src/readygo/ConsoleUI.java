package readygo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 콘솔 입력과 출력만 담당한다. 계산·판단은 하지 않는다.
 * 진행 순서: 로드 → 오늘 날씨 표 → 일정 입력 → 분석 → 추천 → 출력
 */
public class ConsoleUI {

    /** 자동 시험(-Decho=true) 때만 입력값을 화면에 다시 출력한다. */
    private static final boolean ECHO_INPUT = Boolean.getBoolean("echo");

    private static final String BAR = "============================================================";
    private static final String LINE = "------------------------------------------------------------";

    private final Scanner in;
    private final WeatherAnalyzer analyzer;
    private final Recommender recommender;

    public ConsoleUI() {
        this(new Scanner(System.in));
    }

    public ConsoleUI(Scanner in) {
        this.in = in;
        this.analyzer = new WeatherAnalyzer();
        this.recommender = Recommender.withDefaultRules();
    }

    /** 프로그램 전체 흐름. 오류가 나면 안내를 출력하고 돌아온다. */
    public void run(WeatherSource source) {
        System.out.println(BAR);
        System.out.println(" ReadyGo - 날씨 보고 외출 준비하기");
        System.out.println(BAR);

        DailyWeather weather;
        try {
            weather = source.load();
        } catch (WeatherLoadException e) {
            System.out.println("[오류] 날씨 데이터를 불러오지 못했습니다.");
            System.out.println("       " + e.getMessage());
            System.out.println("프로그램을 종료합니다.");
            return;
        }

        printToday(weather);

        OutingPlan plan;
        try {
            plan = readPlan();
        } catch (NoSuchElementException e) { // 입력 스트림이 끝남 (Ctrl+Z / Ctrl+D, 파이프 입력 끝)
            System.out.println();
            System.out.println("[안내] 입력이 끝나서 프로그램을 종료합니다.");
            return;
        }

        WeatherSummary summary;
        try {
            summary = analyzer.analyze(weather, plan);
        } catch (MissingWeatherDataException e) {
            System.out.println();
            System.out.println("[오류] 외출 구간에 날씨 데이터가 없는 시각이 있습니다.");
            System.out.println("       " + e.getMessage());
            System.out.println("분석을 중단합니다. 다른 시간대를 골라 주세요.");
            return;
        }

        Recommendation rec = recommender.recommend(summary, plan);
        printSummary(summary, plan);
        printRecommendation(rec);

        // y면 바로 종료, n이면 이유(와 필요할 때만 안내)를 보여준 뒤 종료한다.
        try {
            if (!askExit()) {
                printDetails(rec);
            }
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("[안내] 입력이 끝나서 프로그램을 종료합니다.");
            return;
        }
        System.out.println("프로그램을 종료합니다.");
        System.out.println(BAR);
    }

    // ------------------------------------------------------------------ 출력

    /** 오늘 최저·최고·일교차와 24시간 표. 빈 시각은 "데이터 없음". */
    public void printToday(DailyWeather weather) {
        List<HourlyWeather> all = weather.all();
        System.out.println();
        System.out.printf("[%s] 오늘의 날씨 (24시간 중 %d시간 데이터)%n", weather.getRegionName(), all.size());

        if (!all.isEmpty()) {
            HourlyWeather lo = all.get(0);
            HourlyWeather hi = all.get(0);
            for (HourlyWeather hw : all) {
                if (hw.temp() < lo.temp()) {
                    lo = hw;
                }
                if (hw.temp() > hi.temp()) {
                    hi = hw;
                }
            }
            System.out.printf("최저 %d°C (%02d시) · 최고 %d°C (%02d시) · 일교차 %d°C%n",
                    lo.temp(), lo.hour(), hi.temp(), hi.hour(), hi.temp() - lo.temp());
        }

        System.out.println(LINE);
        System.out.println(" 시각    기온     체감  강수확률  습도  강수형태");
        System.out.println(LINE);
        for (int h = 0; h < 24; h++) {
            Optional<HourlyWeather> o = weather.get(h);
            if (o.isPresent()) {
                HourlyWeather hw = o.get();
                System.out.printf(" %02d시  %4d°C  %5.1f°C     %3d%%  %3d%%  %s%n",
                        hw.hour(), hw.temp(), hw.feelsLike(), hw.pop(), hw.humidity(),
                        HourlyWeather.ptyName(hw.pty()));
            } else {
                System.out.printf(" %02d시  데이터 없음%n", h);
            }
        }
        System.out.println(LINE);
    }

    /** 외출 구간의 최저·최고·강수·체감온도 */
    public void printSummary(WeatherSummary s, OutingPlan plan) {
        System.out.println();
        System.out.printf("=== 외출 구간 요약: %02d시 ~ %02d시 (%d시간, %s) ===%n",
                plan.start(), plan.end(), plan.hours(), plan.purpose().getDisplayName());
        System.out.printf("  최저 기온     : %d°C (%02d시)%n", s.minTemp(), s.coldest().hour());
        System.out.printf("  최고 기온     : %d°C (%02d시)%n", s.maxTemp(), s.warmest().hour());
        System.out.printf("  기온 차       : %d°C%n", s.tempRange());
        System.out.printf("  최대 강수확률 : %d%%%n", s.maxPop());
        if (s.precipExpected()) {
            String text = s.precipHours().stream()
                    .map(hw -> hw.pty() == 0
                            ? String.format("%d시(%d%%)", hw.hour(), hw.pop())
                            : String.format("%d시(%s %d%%)", hw.hour(), HourlyWeather.ptyName(hw.pty()), hw.pop()))
                    .collect(Collectors.joining(", "));
            System.out.println("  강수 예상     : " + text);
        } else {
            System.out.printf("  강수 예상     : 없음 (강수확률 %d%% 이상인 시각 없음)%n", WeatherAnalyzer.RAIN_POP_THRESHOLD);
        }
        System.out.printf("  최고 체감온도 : %.1f°C (%02d시)%n", s.maxFeelsLike(), s.maxFeelsLikeHour());
    }

    /** 복장 → 준비물 순서로 출력 (이유·안내는 printDetails) */
    public void printRecommendation(Recommendation rec) {
        System.out.println();
        System.out.println("=== 추천 결과 ===");
        System.out.println("[복장]");
        System.out.println("  상의      : " + rec.getTop().orElse("-"));
        System.out.println("  겉옷      : " + rec.getOuter().orElse("-"));
        System.out.println("  하의·신발 : " + rec.getBottomAndShoes().orElse("-"));

        Set<String> items = rec.getItems();
        System.out.printf("[준비물] %d개%n", items.size());
        System.out.println("  " + String.join(", ", items));
    }

    /**
     * 종료할지 묻는다. y/n(대소문자 무관)이 아니면 다시 묻는다.
     *
     * @return y를 입력하면 true
     * @throws NoSuchElementException 입력 스트림이 끝났을 때
     */
    public boolean askExit() {
        System.out.println();
        while (true) {
            System.out.print("종료하시겠습니까? (y/n): ");
            String line = in.nextLine().trim();
            if (ECHO_INPUT) {
                System.out.println(line);
            }
            if (line.equalsIgnoreCase("y")) {
                return true;
            }
            if (line.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("  y 또는 n으로 입력해 주세요.");
        }
    }

    /** 이유 → 안내 순서로 출력. 안내는 알릴 내용이 있을 때만 출력한다. */
    public void printDetails(Recommendation rec) {
        System.out.println();
        System.out.println("[이유]");
        List<String> reasons = rec.getReasons();
        for (int i = 0; i < reasons.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, reasons.get(i));
        }

        if (!rec.getNotices().isEmpty()) {
            System.out.println("[안내]");
            for (String notice : rec.getNotices()) {
                System.out.println("  ! " + notice);
            }
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ 입력

    /**
     * 외출·귀가 시각과 목적을 받아 일정을 만든다.
     * 역순이면 처음부터 다시, 같은 시각이면 '잠깐 외출' 메뉴를 보여준다.
     */
    public OutingPlan readPlan() {
        System.out.println();
        System.out.println("외출 일정을 입력하세요. (같은 날 0~23시, 귀가 시각 포함)");
        while (true) {
            int start = readHour("외출 시각(0~23): ");
            int end = readHour("귀가 시각(0~23): ");

            if (start > end) {
                System.out.println("귀가 시각이 외출 시각보다 빠릅니다. 자정을 넘기는 일정은 지원하지 않습니다.");
                System.out.println("처음부터 다시 입력해 주세요.");
                continue;
            }

            if (start == end) {
                System.out.printf("외출 시각과 귀가 시각이 같습니다 (%02d시).%n", start);
                System.out.println("  1. 잠깐 외출로 진행 (공통 준비물만)");
                System.out.println("  2. 처음부터 다시 입력");
                int choice = readMenu("선택(1~2): ", 2);
                if (choice == 1) {
                    return new OutingPlan(start, end, Purpose.QUICK);
                }
                continue;
            }

            Purpose purpose = readPurpose();
            return new OutingPlan(start, end, purpose);
        }
    }

    /** 메뉴에서 목적 하나를 고른다 (1~3). */
    public Purpose readPurpose() {
        List<Purpose> options = Purpose.selectable();
        System.out.println("외출 목적을 고르세요.");
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, options.get(i).getDisplayName());
        }
        int choice = readMenu("선택(1~" + options.size() + "): ", options.size());
        return options.get(choice - 1);
    }

    private int readHour(String prompt) {
        return readInt(prompt, 0, 23);
    }

    private int readMenu(String prompt, int count) {
        return readInt(prompt, 1, count);
    }

    /**
     * 숫자이고 범위 안일 때까지 반복해서 묻는다.
     *
     * @throws NoSuchElementException 입력 스트림이 끝났을 때
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();   // Enter 칠 때까지 대기
            if (ECHO_INPUT) {
                System.out.println(line);
            }
            int v;
            try {
                v = Integer.parseInt(line);       // 숫자가 아니면 예외 → 다시 질문
            } catch (NumberFormatException e) {
                System.out.printf("  숫자로 입력해 주세요. (%d~%d)%n", min, max);
                continue;
            }
            if (v >= min && v <= max) {
                return v;
            }
            System.out.printf("  %d~%d 사이로 입력해 주세요.%n", min, max); // 범위 밖이면 다시 질문
        }
    }
}
