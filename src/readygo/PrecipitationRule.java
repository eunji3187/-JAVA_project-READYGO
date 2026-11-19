package readygo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 강수 규칙.
 * <ul>
 *   <li>강수 시각이 없으면 "우산 제외" 이유만 남긴다.</li>
 *   <li>있으면 우산. 그 중 비가 있으면 장화도.</li>
 *   <li>눈이 있으면 폭설(적설량) 기준은 적용하지 않는다는 안내를 남긴다.</li>
 * </ul>
 */
public class PrecipitationRule implements Rule {

    @Override
    public void apply(WeatherSummary summary, OutingPlan plan, Recommendation rec) {
        int threshold = WeatherAnalyzer.RAIN_POP_THRESHOLD;

        if (!summary.precipExpected()) {
            rec.addReason(String.format("최대 강수확률 %d%%로 %d%% 미만 → 우산 제외", summary.maxPop(), threshold));
            return;
        }

        List<HourlyWeather> precip = summary.precipHours();
        rec.addItem("우산");
        rec.addReason(String.format("%s 강수확률 %d%% 이상(최대 %d%%) → 우산",
                hoursText(precip), threshold, summary.maxPop()));

        if (summary.rainExpected()) {
            List<HourlyWeather> rain = precip.stream().filter(HourlyWeather::isRainType).collect(Collectors.toList());
            rec.addItem("장화");
            rec.addReason(String.format("%s 비 예보 → 장화", hoursText(rain)));
        }

        if (summary.snowExpected()) {
            List<HourlyWeather> snow = precip.stream().filter(HourlyWeather::isSnowType).collect(Collectors.toList());
            rec.addNotice(String.format("%s 눈 예보가 있습니다. 적설량(폭설) 기준은 적용하지 않았으니 기상특보를 따로 확인하세요",
                    hoursText(snow)));
        }
    }

    /** [15시, 16시, 17시] → "15·16·17시" */
    private static String hoursText(List<HourlyWeather> hours) {
        return hours.stream()
                .map(hw -> String.valueOf(hw.hour()))
                .collect(Collectors.joining("·")) + "시";
    }
}
