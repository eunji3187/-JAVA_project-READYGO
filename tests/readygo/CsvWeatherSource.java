package readygo;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * [시험 전용] CSV 파일에서 하루치 날씨를 읽는 공급원.
 * 프로그램 본체는 기상청 API만 쓰고, 이 클래스는 자동 시험에서 고정된 날씨를 넣기 위해서만 쓴다.
 *
 * <pre>
 * # region=서울            ← 지역 이름 (선택)
 * hour,temp,pop,pty,humidity  ← 헤더 (건너뜀)
 * 15,17,60,1,70            ← 15시, 17°C, 강수확률 60%, 비(1), 습도 70%
 * </pre>
 */
public class CsvWeatherSource implements WeatherSource {

    private static final int COLUMNS = 5;
    private static final String[] COLUMN_NAMES = {"시각", "기온", "강수확률", "강수형태", "습도"};
    private static final String REGION_KEY = "region=";
    private static final String DEFAULT_REGION = "지역 미상";

    private final Path path;

    public CsvWeatherSource(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public CsvWeatherSource(String path) {
        this(Path.of(path));
    }

    public Path getPath() {
        return path;
    }

    @Override
    public DailyWeather load() throws WeatherLoadException {
        // 1. 파일 전체를 UTF-8로 읽는다.
        List<String> lines = readLines();

        String region = DEFAULT_REGION;
        boolean headerSkipped = false;
        Map<Integer, HourlyWeather> byHour = new TreeMap<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            if (i == 0 && line.startsWith("﻿")) { // 메모장이 붙이는 BOM 제거
                line = line.substring(1);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 2. '#' 줄은 주석. 단 '# region=지역명'은 지역 이름으로 저장.
            if (line.startsWith("#")) {
                String comment = line.substring(1).trim();
                if (comment.startsWith(REGION_KEY)) {
                    String name = comment.substring(REGION_KEY.length()).trim();
                    if (!name.isEmpty()) {
                        region = name;
                    }
                }
                continue;
            }

            // 3. 주석이 아닌 첫 줄은 헤더라서 건너뛴다.
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }

            // 4. 칸 수 → 숫자 → 범위 → 중복 순서로 검사.
            HourlyWeather hw = parseLine(line, lineNo);
            if (byHour.containsKey(hw.hour())) {
                throw error(lineNo, hw.hour() + "시가 두 번 나왔습니다 (같은 시각 중복)");
            }
            byHour.put(hw.hour(), hw);
        }

        if (byHour.isEmpty()) {
            throw new WeatherLoadException(path + ": 날씨 데이터 줄이 하나도 없습니다");
        }

        // 5. 통과한 줄들로 DailyWeather를 만든다.
        return new DailyWeather(region, byHour);
    }

    private List<String> readLines() throws WeatherLoadException {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new WeatherLoadException("날씨 파일을 찾을 수 없습니다: " + path, e);
        } catch (MalformedInputException e) {
            throw new WeatherLoadException("날씨 파일이 UTF-8 형식이 아닙니다: " + path, e);
        } catch (IOException e) {
            throw new WeatherLoadException("날씨 파일을 읽을 수 없습니다: " + path + " (" + e + ")", e);
        }
    }

    private HourlyWeather parseLine(String line, int lineNo) throws WeatherLoadException {
        String[] cells = line.split(",", -1);
        if (cells.length != COLUMNS) {
            throw error(lineNo, "칸이 " + COLUMNS + "개여야 하는데 " + cells.length + "개입니다");
        }

        int[] v = new int[COLUMNS];
        for (int k = 0; k < COLUMNS; k++) {
            String cell = cells[k].trim();
            try {
                v[k] = Integer.parseInt(cell);
            } catch (NumberFormatException e) {
                throw error(lineNo, COLUMN_NAMES[k] + " 값 '" + cell + "'을(를) 정수로 바꿀 수 없습니다");
            }
        }

        checkRange(lineNo, 0, v[0], 0, 23);   // 시각
        checkRange(lineNo, 2, v[2], 0, 100);  // 강수확률
        checkRange(lineNo, 3, v[3], 0, 4);    // 강수형태
        checkRange(lineNo, 4, v[4], 0, 100);  // 습도

        return new HourlyWeather(v[0], v[1], v[2], v[3], v[4]);
    }

    private void checkRange(int lineNo, int column, int value, int min, int max) throws WeatherLoadException {
        if (value < min || value > max) {
            throw error(lineNo, COLUMN_NAMES[column] + " 값 " + value + "이(가) 범위(" + min + "~" + max + ") 밖입니다");
        }
    }

    private WeatherLoadException error(int lineNo, String detail) {
        return new WeatherLoadException(path + " " + lineNo + "번째 줄: " + detail);
    }
}
