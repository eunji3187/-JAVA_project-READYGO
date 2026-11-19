package readygo;

import java.util.List;

/** 목적 준비물 규칙. 항상 공통 준비물 3개를 넣고, 목적별 준비물을 더한다. */
public class PurposeItemRule implements Rule {

    /** 목적과 상관없이 늘 챙기는 준비물 */
    public static final List<String> COMMON_ITEMS = List.of("휴대폰", "지갑", "열쇠");

    @Override
    public void apply(WeatherSummary summary, OutingPlan plan, Recommendation rec) {
        for (String item : COMMON_ITEMS) {
            rec.addItem(item);
        }
        Purpose purpose = plan.purpose();
        for (String item : purpose.getItems()) {
            rec.addItem(item);
        }

        String common = String.join("·", COMMON_ITEMS);
        if (purpose.getItems().isEmpty()) {
            rec.addReason(String.format("목적 '%s' → 공통 준비물(%s)만", purpose.getDisplayName(), common));
        } else {
            rec.addReason(String.format("목적 '%s' → 공통 준비물(%s) + 목적 준비물(%s)",
                    purpose.getDisplayName(), common, String.join("·", purpose.getItems())));
        }
    }
}
