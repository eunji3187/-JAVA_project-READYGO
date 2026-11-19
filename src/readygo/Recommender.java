package readygo;

import java.util.List;

/** 등록된 규칙을 순서대로 적용해 추천 결과를 만든다. */
public class Recommender {

    private final List<Rule> rules;

    /** 규칙 목록은 복사해 두므로 만든 뒤 바뀌지 않는다. */
    public Recommender(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** 복장 → 목적 → 강수 → 폭염 순서로 규칙을 등록한다. */
    public static Recommender withDefaultRules() {
        return new Recommender(List.of(
                new OutfitRule(),
                new PurposeItemRule(),
                new PrecipitationRule(),
                new HeatRule()));
    }

    /** 빈 결과 그릇을 만들고 규칙들이 차례로 채운다. */
    public Recommendation recommend(WeatherSummary summary, OutingPlan plan) {
        Recommendation rec = new Recommendation();   // 빈 그릇
        for (Rule r : rules) {
            r.apply(summary, plan, rec);             // 규칙들이 차례로 채움
        }
        return rec;
    }
}
