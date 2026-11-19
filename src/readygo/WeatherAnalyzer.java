package readygo;

import java.util.ArrayList;
import java.util.List;

/** 하루 날씨와 외출 일정을 받아 외출 구간을 요약한다. */
public class WeatherAnalyzer {

    /** 이 강수확률(%) 이상이면 '강수 예상 시각'으로 본다. */
    public static final int RAIN_POP_THRESHOLD = 50;

    /**
     * @throws MissingWeatherDataException 외출 구간에 빈 시각이 있으면 (그대로 콘솔까지 올라간다)
     */
    public WeatherSummary analyze(DailyWeather weather, OutingPlan plan) throws MissingWeatherDataException {
        // 1. 외출 구간(귀가 시각 포함)을 받는다.
        List<HourlyWeather> hours = weather.range(plan.start(), plan.end());

        // 2. 한 번 돌면서 최저·최고 기온, 최대 강수확률, 최고 체감온도를 찾는다.
        HourlyWeather coldest = hours.get(0);
        HourlyWeather warmest = hours.get(0);
        int maxPop = 0;
        double maxFeelsLike = hours.get(0).feelsLike();
        int maxFeelsLikeHour = hours.get(0).hour();
        // 3. 강수확률이 기준 이상인 시각만 모은다.
        List<HourlyWeather> precipHours = new ArrayList<>();

        for (HourlyWeather hw : hours) {
            if (hw.temp() < coldest.temp()) {
                coldest = hw;
            }
            if (hw.temp() > warmest.temp()) {
                warmest = hw;
            }
            if (hw.pop() > maxPop) {
                maxPop = hw.pop();
            }
            double feels = hw.feelsLike();
            if (feels > maxFeelsLike) {
                maxFeelsLike = feels;
                maxFeelsLikeHour = hw.hour();
            }
            if (hw.pop() >= RAIN_POP_THRESHOLD) {
                precipHours.add(hw);
            }
        }

        // 4. WeatherSummary로 묶어 돌려준다.
        return new WeatherSummary(coldest, warmest, precipHours, maxPop, maxFeelsLike, maxFeelsLikeHour);
    }
}
