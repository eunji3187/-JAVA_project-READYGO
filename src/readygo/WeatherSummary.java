package readygo;

import java.util.List;
import java.util.Objects;

/**
 * 외출 구간 날씨 요약. WeatherAnalyzer가 만들고 규칙(Rule)들이 읽는다.
 *
 * @param coldest          가장 추운 시각의 날씨 (같으면 이른 시각)
 * @param warmest          가장 더운 시각의 날씨 (같으면 이른 시각)
 * @param precipHours      강수확률이 기준 이상인 시각들
 * @param maxPop           최대 강수확률
 * @param maxFeelsLike     최고 체감온도
 * @param maxFeelsLikeHour 최고 체감온도 시각
 */
public record WeatherSummary(
        HourlyWeather coldest,
        HourlyWeather warmest,
        List<HourlyWeather> precipHours,
        int maxPop,
        double maxFeelsLike,
        int maxFeelsLikeHour) {

    public WeatherSummary {
        Objects.requireNonNull(coldest, "coldest");
        Objects.requireNonNull(warmest, "warmest");
        precipHours = List.copyOf(precipHours);
    }

    public int minTemp() {
        return coldest.temp();
    }

    public int maxTemp() {
        return warmest.temp();
    }

    /** 외출 구간 기온 차 (최고 − 최저) */
    public int tempRange() {
        return maxTemp() - minTemp();
    }

    /** 강수확률 기준 이상인 시각이 하나라도 있으면 true */
    public boolean precipExpected() {
        return !precipHours.isEmpty();
    }

    /** 강수 시각 중 비(비·비/눈·소나기)가 있으면 true */
    public boolean rainExpected() {
        return precipHours.stream().anyMatch(HourlyWeather::isRainType);
    }

    /** 강수 시각 중 눈(비/눈·눈)이 있으면 true */
    public boolean snowExpected() {
        return precipHours.stream().anyMatch(HourlyWeather::isSnowType);
    }
}
