package readygo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 하루치 날씨. 시각 → HourlyWeather 를 TreeMap(시각 순 정렬)으로 들고 있고, 만든 뒤에는 수정할 수 없다.
 * 일부 시각이 빠져 있을 수 있다.
 */
public class DailyWeather {

    private final String regionName;
    private final SortedMap<Integer, HourlyWeather> byHour;

    public DailyWeather(String regionName, Map<Integer, HourlyWeather> hours) {
        this.regionName = Objects.requireNonNull(regionName, "regionName");
        TreeMap<Integer, HourlyWeather> copy = new TreeMap<>();
        for (Map.Entry<Integer, HourlyWeather> e : hours.entrySet()) {
            HourlyWeather hw = Objects.requireNonNull(e.getValue(), "HourlyWeather");
            if (e.getKey() != hw.hour()) {
                throw new IllegalArgumentException("키 " + e.getKey() + "와 시각 " + hw.hour() + "이 다릅니다");
            }
            copy.put(hw.hour(), hw);
        }
        this.byHour = Collections.unmodifiableSortedMap(copy);
    }

    public String getRegionName() {
        return regionName;
    }

    /** 그 시각 날씨. 없으면 빈 Optional. */
    public Optional<HourlyWeather> get(int hour) {
        return Optional.ofNullable(byHour.get(hour));
    }

    /** 가지고 있는 모든 시각의 날씨 (시각 순, 수정 불가) */
    public List<HourlyWeather> all() {
        return List.copyOf(byHour.values());
    }

    /**
     * start 시부터 end 시까지(귀가 시각 포함) 날씨 목록.
     *
     * @throws MissingWeatherDataException 구간 안에 빈 시각이 하나라도 있으면 (누락 시각 목록 포함)
     */
    public List<HourlyWeather> range(int start, int end) throws MissingWeatherDataException {
        List<HourlyWeather> result = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();
        for (int h = start; h <= end; h++) {
            HourlyWeather hw = byHour.get(h);
            if (hw == null) {
                missing.add(h);
            } else {
                result.add(hw);
            }
        }
        if (!missing.isEmpty()) {
            throw new MissingWeatherDataException(missing);
        }
        return List.copyOf(result);
    }
}
