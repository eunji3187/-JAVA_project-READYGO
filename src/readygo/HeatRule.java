package readygo;

/** 폭염 규칙. 최고 체감온도가 기준 이상이면 물병을 넣는다 (이미 있으면 중복 추가하지 않음). */
public class HeatRule implements Rule {

    /** 폭염 판단 체감온도 (°C) */
    public static final double HEAT_FEELS_LIKE = 33.0;

    private static final String WATER = "물병";

    @Override
    public void apply(WeatherSummary summary, OutingPlan plan, Recommendation rec) {
        if (summary.maxFeelsLike() < HEAT_FEELS_LIKE) {
            return;
        }
        String cause = String.format("최고 체감온도 %.1f°C(%02d시)가 폭염 기준 %.0f°C 이상",
                summary.maxFeelsLike(), summary.maxFeelsLikeHour(), HEAT_FEELS_LIKE);
        if (rec.addItem(WATER)) {
            rec.addReason(cause + " → 물병");
        } else {
            rec.addReason(cause + " → 물병은 이미 준비물에 있어 중복 추가하지 않음");
        }
    }
}
