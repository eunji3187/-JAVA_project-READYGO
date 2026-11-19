import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import readygo.ClothingBand;
import readygo.CsvWeatherSource;
import readygo.DailyWeather;
import readygo.HeatRule;
import readygo.HourlyWeather;
import readygo.MissingWeatherDataException;
import readygo.OutingPlan;
import readygo.Purpose;
import readygo.Recommendation;
import readygo.Recommender;
import readygo.WeatherAnalyzer;
import readygo.WeatherLoadException;
import readygo.WeatherSummary;

/**
 * ReadyGo 완료 기준 자동 시험.
 *
 * <p>프로젝트 폴더(readygo/)에서 실행한다. tests/run_tests.bat 또는 tests/run_tests.sh 사용.
 * <ul>
 *   <li>U-xx: 클래스를 직접 불러 값 확인 (단위 시험)</li>
 *   <li>C-xx: 프로그램을 따로 띄워 입력을 넣고 화면 출력 확인 (콘솔 시험, -Decho=true)</li>
 * </ul>
 * 콘솔 시험의 화면 기록은 tests/results/C-xx.txt, 전체 결과는 tests/results/summary.txt 에 남는다.
 */
public class ReadyGoTests {

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RESULTS = ROOT.resolve("tests").resolve("results");
    private static final Path FIXTURES = ROOT.resolve("tests").resolve("fixtures");

    private static final List<String> report = new ArrayList<>();
    private static final Map<String, Boolean> caseResult = new HashMap<>();
    private static String currentCase;
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULTS);
        report.add("ReadyGo 자동 시험 결과  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        report.add("Java " + System.getProperty("java.version"));
        report.add("");

        unitTests();
        consoleTests();

        report.add("");
        report.add(String.format("합계: 시험 항목 %d개 통과 / %d개 실패 (확인 %d개 통과, %d개 실패)",
                caseResult.values().stream().filter(b -> b).count(),
                caseResult.values().stream().filter(b -> !b).count(),
                passed, failed));
        Files.write(RESULTS.resolve("summary.txt"), report, StandardCharsets.UTF_8);
        for (String line : report) {
            System.out.println(line);
        }
        System.exit(failed == 0 ? 0 : 1);
    }

    // ===================================================================== 단위 시험

    private static void unitTests() throws Exception {
        start("U-01", "체감온도: 기상청 여름철 산출식 (35°C·56% → 35.1, 17°C·70% → 18.2)");
        check("35°C 56%", 35.1, new HourlyWeather(14, 35, 0, 0, 56).feelsLike());
        check("17°C 70%", 18.2, new HourlyWeather(15, 17, 60, 1, 70).feelsLike());

        start("U-02", "강수형태 판정과 이름 (비: 1·2·4, 눈: 2·3)");
        boolean[] rain = {false, true, true, false, true};
        boolean[] snow = {false, false, true, true, false};
        String[] names = {"없음", "비", "비/눈", "눈", "소나기"};
        for (int p = 0; p <= 4; p++) {
            HourlyWeather hw = new HourlyWeather(0, 10, 0, p, 50);
            check("pty " + p + " isRainType", rain[p], hw.isRainType());
            check("pty " + p + " isSnowType", snow[p], hw.isSnowType());
            check("pty " + p + " 이름", names[p], HourlyWeather.ptyName(p));
        }

        start("U-03", "옷차림 구간 경계: 모든 정수 기온이 7개 구간 중 하나에 속함");
        check("2°C", ClothingBand.FREEZING, ClothingBand.of(2));
        check("3°C", ClothingBand.COLD, ClothingBand.of(3));
        check("10°C", ClothingBand.COLD, ClothingBand.of(10));
        check("11°C", ClothingBand.COOL, ClothingBand.of(11));
        check("14°C", ClothingBand.COOL, ClothingBand.of(14));
        check("15°C", ClothingBand.MILD, ClothingBand.of(15));
        check("19°C", ClothingBand.MILD, ClothingBand.of(19));
        check("20°C", ClothingBand.WARM, ClothingBand.of(20));

        check("경계 Integer.MIN_VALUE", ClothingBand.FREEZING, ClothingBand.of(Integer.MIN_VALUE));
        check("경계 22", ClothingBand.WARM, ClothingBand.of(22));
        check("경계 23", ClothingBand.HOT, ClothingBand.of(23));
        check("경계 27", ClothingBand.HOT, ClothingBand.of(27));
        check("경계 28", ClothingBand.VERY_HOT, ClothingBand.of(28));
        check("경계 Integer.MAX_VALUE", ClothingBand.VERY_HOT, ClothingBand.of(Integer.MAX_VALUE));

        start("U-04", "목적 메뉴는 3개 (잠깐 외출 제외)");
        check("selectable", List.of(Purpose.STUDY, Purpose.WORK, Purpose.SHOPPING), Purpose.selectable());
        check("QUICK 준비물 없음", true, Purpose.QUICK.getItems().isEmpty());

        start("U-05", "일정 검사: hours(), 역순·범위 밖이면 IllegalArgumentException");
        check("9~21 hours", 13, new OutingPlan(9, 21, Purpose.STUDY).hours());
        check("10~10 hours", 1, new OutingPlan(10, 10, Purpose.QUICK).hours());
        checkThrows("21~9 역순", IllegalArgumentException.class, () -> new OutingPlan(21, 9, Purpose.STUDY));
        checkThrows("24시", IllegalArgumentException.class, () -> new OutingPlan(9, 24, Purpose.STUDY));
        checkThrows("-1시", IllegalArgumentException.class, () -> new OutingPlan(-1, 3, Purpose.STUDY));

        start("U-06", "Recommendation: 중복 준비물은 false, 순서 유지, 목록은 수정 불가");
        Recommendation r = new Recommendation();
        check("첫 물병", true, r.addItem("물병"));
        check("두 번째 물병", false, r.addItem("물병"));
        r.addItem("우산");
        check("순서", List.of("물병", "우산"), new ArrayList<>(r.getItems()));
        checkThrows("items 수정", UnsupportedOperationException.class, () -> r.getItems().add("x"));
        checkThrows("reasons 수정", UnsupportedOperationException.class, () -> r.getReasons().add("x"));
        check("복장 없음 → 빈 Optional", Optional.empty(), r.getTop());

        start("U-07", "CSV 로드: 대표 시나리오 A (지역, 24시간, 15시 값)");
        DailyWeather a = new CsvWeatherSource("tests/data/scenario_a.csv").load();
        check("지역", "서울", a.getRegionName());
        check("시간 수", 24, a.all().size());
        check("15시", Optional.of(new HourlyWeather(15, 17, 60, 1, 70)), a.get(15));

        start("U-08", "CSV 로드: BOM·CRLF·빈 줄·칸 사이 공백 허용");
        DailyWeather bom = new CsvWeatherSource(FIXTURES.resolve("bom_crlf.csv")).load();
        check("지역", "부산", bom.getRegionName());
        check("10시", Optional.of(new HourlyWeather(10, 16, 20, 0, 50)), bom.get(10));
        check("시간 수", 2, bom.all().size());

        start("U-09", "CSV 오류: 몇 번째 줄인지 담아 WeatherLoadException");
        checkLoadError("파일 없음", "tests/data/no_such_file.csv", "찾을 수 없습니다");
        checkLoadError("칸 수", FIXTURES.resolve("bad_columns.csv").toString(), "4번째 줄: 칸이 5개여야 하는데 4개");
        checkLoadError("숫자", FIXTURES.resolve("bad_number.csv").toString(), "4번째 줄: 기온 값 'seven'");
        checkLoadError("강수형태 범위", FIXTURES.resolve("bad_range_pty.csv").toString(), "4번째 줄: 강수형태 값 5");
        checkLoadError("시각 범위", FIXTURES.resolve("bad_range_hour.csv").toString(), "5번째 줄: 시각 값 24");
        checkLoadError("시각 중복", FIXTURES.resolve("dup_hour.csv").toString(), "5번째 줄: 1시가 두 번");
        checkLoadError("데이터 없음", FIXTURES.resolve("empty.csv").toString(), "하나도 없습니다");

        start("U-10", "DailyWeather.range(): 누락 시각을 메시지에 담아 MissingWeatherDataException");
        DailyWeather d = new CsvWeatherSource("tests/data/scenario_d.csv").load();
        try {
            d.range(9, 21);
            check("예외 발생", true, false);
        } catch (MissingWeatherDataException e) {
            check("누락 시각", "누락: [12]시", e.getMessage());
            check("메시지", "누락: [12]시", e.getMessage());
        }
        try {
            d.range(0, 23);
            check("예외 발생", true, false);
        } catch (MissingWeatherDataException e) {
            check("누락 시각 0~23", "누락: [3, 12]시", e.getMessage());
        }
        check("13~18 시간 수", 6, d.range(13, 18).size());

        start("U-11", "분석: 대표 시나리오 A 9~21시 요약값");
        OutingPlan planA = new OutingPlan(9, 21, Purpose.STUDY);
        WeatherSummary s = new WeatherAnalyzer().analyze(a, planA);
        check("최저 기온", 9, s.minTemp());
        check("최저 시각", 9, s.coldest().hour());
        check("최고 기온", 17, s.maxTemp());
        check("최고 시각(같으면 이른 시각)", 14, s.warmest().hour());
        check("기온 차", 8, s.tempRange());
        check("최대 강수확률", 70, s.maxPop());
        check("강수 시각", List.of(15, 16, 17), s.precipHours().stream().map(HourlyWeather::hour).toList());
        check("최고 체감온도", 18.2, s.maxFeelsLike());
        check("최고 체감 시각", 15, s.maxFeelsLikeHour());
        check("비 예상", true, s.rainExpected());
        check("눈 예상", false, s.snowExpected());

        start("U-12", "추천: 대표 시나리오 A 최종값 (복장 3종, 준비물 8개, 이유 5줄, 안내 0줄)");
        Recommendation rec = Recommender.withDefaultRules().recommend(s, planA);
        check("상의", Optional.of(ClothingBand.MILD.getTop()), rec.getTop());
        check("겉옷", Optional.of(ClothingBand.COLD.getOuter()), rec.getOuter());
        check("하의·신발", Optional.of(ClothingBand.MILD.getBottomAndShoes()), rec.getBottomAndShoes());
        check("준비물", List.of("휴대폰", "지갑", "열쇠", "노트북", "충전기", "필기구", "우산", "장화"),
                new ArrayList<>(rec.getItems()));
        check("이유 줄 수", 5, rec.getReasons().size());
        check("안내 줄 수", 0, rec.getNotices().size());

        start("U-13", "폭염 규칙: 기준 33°C, 목적에 물병이 있으면 중복 추가하지 않음");
        DailyWeather b = new CsvWeatherSource("tests/data/scenario_b.csv").load();
        check("기준값", 33.0, HeatRule.HEAT_FEELS_LIKE);
        OutingPlan work = new OutingPlan(9, 21, Purpose.WORK);
        Recommendation rw = Recommender.withDefaultRules().recommend(new WeatherAnalyzer().analyze(b, work), work);
        check("출근: 물병 1개", 1L, rw.getItems().stream().filter("물병"::equals).count());
        check("출근: 중복 이유", true, rw.getReasons().stream().anyMatch(x -> x.contains("중복 추가하지 않음")));
        OutingPlan shop = new OutingPlan(9, 21, Purpose.SHOPPING);
        Recommendation rs = Recommender.withDefaultRules().recommend(new WeatherAnalyzer().analyze(b, shop), shop);
        check("쇼핑: 물병 추가", true, rs.getItems().contains("물병"));
        check("쇼핑: 우산 없음", false, rs.getItems().contains("우산"));
        check("폭염 상의", Optional.of(ClothingBand.VERY_HOT.getTop()), rs.getTop());
        check("복장 누락 안내 없음", 0, rs.getNotices().size());

        start("U-14", "강수 규칙: 눈만 오면 우산 O·장화 X·폭설 미적용 안내 / 강수형태 없음이면 우산만");
        DailyWeather c = new CsvWeatherSource("tests/data/scenario_c.csv").load();
        Recommendation rc = Recommender.withDefaultRules().recommend(new WeatherAnalyzer().analyze(c, planA), planA);
        check("우산", true, rc.getItems().contains("우산"));
        check("장화", false, rc.getItems().contains("장화"));
        check("폭설 안내", true, rc.getNotices().stream().anyMatch(x -> x.contains("폭설")));
        OutingPlan d1318 = new OutingPlan(13, 18, Purpose.SHOPPING);
        Recommendation rd = Recommender.withDefaultRules().recommend(new WeatherAnalyzer().analyze(d, d1318), d1318);
        check("D 13~18 우산", true, rd.getItems().contains("우산"));
        check("D 13~18 장화", false, rd.getItems().contains("장화"));
        check("기준 50%", 50, WeatherAnalyzer.RAIN_POP_THRESHOLD);
    }

    // ===================================================================== 콘솔 시험

    private static void consoleTests() throws Exception {
        String out;

        start("C-01", "대표 시나리오 A, 9~21시 공부 → n이면 이유 출력, 안내 0건이면 [안내] 생략");
        out = runConsole("C-01", "9\n21\n1\nn\n", "tests/data/scenario_a.csv");
        contains(out, "[서울] 오늘의 날씨 (24시간 중 24시간 데이터)");
        contains(out, "최저 6°C (03시) · 최고 17°C (14시) · 일교차 11°C");
        contains(out, "=== 외출 구간 요약: 09시 ~ 21시 (13시간, 공부) ===");
        contains(out, "최저 기온     : 9°C (09시)");
        contains(out, "최고 기온     : 17°C (14시)");
        contains(out, "기온 차       : 8°C");
        contains(out, "최대 강수확률 : 70%");
        contains(out, "15시(비 60%), 16시(비 70%), 17시(비 60%)");
        contains(out, "최고 체감온도 : 18.2°C (15시)");
        contains(out, "상의      : 긴팔 티셔츠·얇은 니트");
        contains(out, "겉옷      : 코트·경량 패딩·두꺼운 재킷");
        contains(out, "하의·신발 : 슬랙스·청바지·로퍼");
        contains(out, "[준비물] 8개");
        contains(out, "휴대폰, 지갑, 열쇠, 노트북, 충전기, 필기구, 우산, 장화");
        contains(out, "  5. 15·16·17시 비 예보 → 장화");
        notContains(out, "  6. ");
        notContains(out, "[안내]");
        contains(out, "종료하시겠습니까? (y/n): n");
        inOrder(out, "[복장]", "[준비물]", "종료하시겠습니까? (y/n)", "[이유]", "프로그램을 종료합니다.");

        start("C-02", "종료 질문에서 입력이 끝나도 정상 종료 + 본 프로그램(Main)은 지원하지 않는 지역을 거절");
        out = runConsole("C-02", "9\n21\n1\n", "tests/data/scenario_a.csv");
        contains(out, "[서울] 오늘의 날씨");
        contains(out, "[준비물] 8개");
        contains(out, "[안내] 입력이 끝나서 프로그램을 종료합니다.");
        notContains(out, "[이유]");
        notContains(out, "Exception");
        out = runProgram("C-02-main", "readygo.Main", "", "없는지역");
        contains(out, "[오류] 지원하지 않는 지역: 없는지역");
        contains(out, "지원 지역: 서울");
        notContains(out, "Exception");

        start("C-03", "더운 날: 폭염 복장, 물병 중복 추가 안 함, 우산 제외");
        out = runConsole("C-03", "9\n21\n2\nn\n", "tests/data/scenario_b.csv");
        contains(out, "최고 체감온도 : 35.1°C (14시)");
        contains(out, "→ 물병은 이미 준비물에 있어 중복 추가하지 않음");
        contains(out, "최대 강수확률 30%로 50% 미만 → 우산 제외");
        contains(out, "상의      : " + ClothingBand.VERY_HOT.getTop());
        contains(out, "겉옷      : " + ClothingBand.VERY_HOT.getOuter());
        contains(out, "[준비물] 6개");

        start("C-04", "추운 날: 방한 겉옷, 눈 예보면 우산만·폭설 안내");
        out = runConsole("C-04", "9\n21\n1\nn\n", "tests/data/scenario_c.csv");
        contains(out, "[강릉] 오늘의 날씨");
        contains(out, "10·11·12·13·14시 강수확률 50% 이상(최대 80%) → 우산");
        notContains(out, "장화");
        contains(out, "적설량(폭설) 기준은 적용하지 않았으니");
        contains(out, "겉옷      : " + ClothingBand.FREEZING.getOuter());

        start("C-05", "시나리오 D 9~21시 → \"누락: [12]시\" 후 분석 중단, 표에는 \"데이터 없음\"");
        out = runConsole("C-05", "9\n21\n3\n", "tests/data/scenario_d.csv");
        contains(out, "[인천] 오늘의 날씨 (24시간 중 22시간 데이터)");
        contains(out, " 03시  데이터 없음");
        contains(out, " 12시  데이터 없음");
        contains(out, "누락: [12]시");
        contains(out, "분석을 중단합니다");
        notContains(out, "=== 추천 결과 ===");

        start("C-06", "시나리오 D 13~18시 쇼핑 → 누락 없는 구간은 정상 추천");
        out = runConsole("C-06", "13\n18\n3\ny\n", "tests/data/scenario_d.csv");
        contains(out, "강수 예상     : 15시(50%), 16시(50%)");
        contains(out, "상의      : 셔츠·얇은 니트");
        contains(out, "겉옷      : 트렌치코트·블레이저·데님 재킷");
        contains(out, "[준비물] 7개");
        notContains(out, "장화");

        start("C-07", "잘못된 입력 반복 질문: 글자·범위 밖·역순(처음부터)·같은 시각(잠깐 외출)");
        out = runConsole("C-07", "abc\n25\n\n21\n9\n10\n10\n3\n1\nn\n", "tests/data/scenario_a.csv");
        contains(out, "외출 시각(0~23): abc\n  숫자로 입력해 주세요. (0~23)");
        contains(out, "외출 시각(0~23): 25\n  0~23 사이로 입력해 주세요.");
        contains(out, "귀가 시각이 외출 시각보다 빠릅니다");
        contains(out, "처음부터 다시 입력해 주세요.");
        contains(out, "외출 시각과 귀가 시각이 같습니다 (10시).");
        contains(out, "선택(1~2): 3\n  1~2 사이로 입력해 주세요.");
        contains(out, "=== 외출 구간 요약: 10시 ~ 10시 (1시간, 잠깐 외출) ===");
        contains(out, "[준비물] 3개");
        contains(out, "목적 '잠깐 외출' → 공통 준비물(휴대폰·지갑·열쇠)만");
        notContains(out, "외출 목적을 고르세요.");

        start("C-08", "같은 시각 → 2번(다시 입력) → 목적 메뉴 범위 밖 재질문 → 출근");
        out = runConsole("C-08", "12\n12\n2\n9\n18\n0\n4\n2\ny\n", "tests/data/scenario_a.csv");
        contains(out, "  2. 처음부터 다시 입력");
        contains(out, "선택(1~3): 0\n  1~3 사이로 입력해 주세요.");
        contains(out, "선택(1~3): 4\n  1~3 사이로 입력해 주세요.");
        contains(out, "=== 외출 구간 요약: 09시 ~ 18시 (10시간, 출근) ===");
        contains(out, "사원증");

        start("C-09", "날씨 파일 없음 → 오류 출력 후 종료");
        out = runConsole("C-09", "", "tests/data/no_such_file.csv");
        contains(out, "[오류] 날씨 데이터를 불러오지 못했습니다.");
        contains(out, "날씨 파일을 찾을 수 없습니다");
        contains(out, "프로그램을 종료합니다.");
        notContains(out, "외출 시각");

        start("C-10", "CSV 형식 오류 → 몇 번째 줄인지 출력 후 종료");
        out = runConsole("C-10", "", FIXTURES.resolve("dup_hour.csv").toString());
        contains(out, "5번째 줄: 1시가 두 번 나왔습니다");
        contains(out, "프로그램을 종료합니다.");

        start("C-11", "입력 도중 입력이 끝나면(EOF) 예외 없이 종료");
        out = runConsole("C-11", "9\n", "tests/data/scenario_a.csv");
        contains(out, "[안내] 입력이 끝나서 프로그램을 종료합니다.");
        notContains(out, "Exception");

        start("C-12", "종료 질문: y/n 외 입력은 재질문, y를 고르면 이유·안내 출력 없이 종료");
        out = runConsole("C-12", "9\n21\n2\n5\nY\n", "tests/data/scenario_b.csv");
        contains(out, "[준비물] 6개");
        contains(out, "종료하시겠습니까? (y/n): 5\n  y 또는 n으로 입력해 주세요.");
        notContains(out, "[이유]");
        notContains(out, "! 최고 기온");
        contains(out, "프로그램을 종료합니다.");
    }

    // ===================================================================== 도구

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void start(String id, String title) {
        currentCase = id;
        caseResult.put(id, true);
        report.add("[" + id + "] " + title);
    }

    private static void record(boolean ok, String what) {
        if (ok) {
            passed++;
        } else {
            failed++;
            caseResult.put(currentCase, false);
        }
        report.add("    " + (ok ? "PASS" : "FAIL") + "  " + what);
    }

    private static void check(String what, Object expected, Object actual) {
        boolean ok = expected.equals(actual);
        record(ok, what + (ok ? " = " + actual : " — 기대 " + expected + ", 실제 " + actual));
    }

    private static void checkThrows(String what, Class<? extends Throwable> type, ThrowingRunnable body) {
        try {
            body.run();
            record(false, what + " — 예외가 나지 않음");
        } catch (Throwable t) {
            record(type.isInstance(t), what + " → " + t.getClass().getSimpleName());
        }
    }

    private static void checkLoadError(String what, String path, String expectedPart) {
        try {
            new CsvWeatherSource(path).load();
            record(false, what + " — 예외가 나지 않음");
        } catch (WeatherLoadException e) {
            boolean ok = e.getMessage().contains(expectedPart);
            String msg = e.getMessage().replace(ROOT.toString(), ".");
            record(ok, what + " → " + msg + (ok ? "" : "  (기대 포함: " + expectedPart + ")"));
        }
    }

    private static void contains(String out, String expected) {
        record(out.contains(expected), "출력에 포함: " + oneLine(expected));
    }

    private static void notContains(String out, String unexpected) {
        record(!out.contains(unexpected), "출력에 없음: " + oneLine(unexpected));
    }

    private static void inOrder(String out, String... parts) {
        int pos = -1;
        boolean ok = true;
        for (String p : parts) {
            int next = out.indexOf(p, pos + 1);
            if (next < 0) {
                ok = false;
                break;
            }
            pos = next;
        }
        record(ok, "출력 순서: " + String.join(" → ", parts));
    }

    private static String oneLine(String s) {
        return s.replace("\n", "\\n");
    }

    /** 프로그램을 따로 실행해 input을 넣고 화면 출력을 돌려준다. 기록은 tests/results/<name>.txt */
    private static String runConsole(String name, String input, String... args) throws IOException, InterruptedException {
        return runProgram(name, "readygo.TestMain", input, args);
    }

    /** mainClass를 따로 실행해 input을 넣고 화면 출력을 돌려준다. */
    private static String runProgram(String name, String mainClass, String input, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-Decho=true");
        cmd.add("-Dstdout.encoding=UTF-8");
        cmd.add("-Dstderr.encoding=UTF-8");
        cmd.add("-cp");
        cmd.add(ROOT.resolve("out") + java.io.File.pathSeparator + ROOT.resolve("out").resolve("tests"));
        cmd.add(mainClass);
        cmd.addAll(List.of(args));

        Process p = new ProcessBuilder(cmd).directory(ROOT.toFile()).redirectErrorStream(true).start();
        try (OutputStream os = p.getOutputStream()) {
            os.write(input.getBytes(StandardCharsets.UTF_8));
        }
        String out;
        try (InputStream is = p.getInputStream()) {
            out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            out += "\n[시험] 30초 안에 끝나지 않아 강제 종료";
        }
        out = out.replace("\r\n", "\n");

        String argText = args.length == 0 ? "(인자 없음)" : String.join(" ", args).replace(ROOT.toString(), ".");
        String header = "# " + name + "\n# 실행: java -Decho=true -cp out;out/tests " + mainClass + " " + argText
                + "\n# 입력: " + oneLine(input) + "\n# 종료 코드: " + p.exitValue() + "\n\n";
        Files.writeString(RESULTS.resolve(name + ".txt"), header + out, StandardCharsets.UTF_8);
        return out;
    }
}
