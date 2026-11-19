package readygo;

/**
 * 복장 규칙.
 * 상의·하의(신발)는 가장 더운 시각 기준, 겉옷은 가장 추운 시각 기준으로 고른다.
 */
public class OutfitRule implements Rule {

    @Override
    public void apply(WeatherSummary summary, OutingPlan plan, Recommendation rec) {
        int max = summary.maxTemp();
        ClothingBand warmBand = ClothingBand.of(max);
        rec.setTop(warmBand.getTop());
        rec.setBottomAndShoes(warmBand.getBottomAndShoes());
        rec.addReason(String.format("최고 기온 %d°C(%02d시)가 %s 구간 → 상의·하의는 이 구간 기준",
                max, summary.warmest().hour(), warmBand.getRangeText()));

        int min = summary.minTemp();
        ClothingBand coldBand = ClothingBand.of(min);
        rec.setOuter(coldBand.getOuter());
        rec.addReason(String.format("최저 기온 %d°C(%02d시)가 %s 구간 → 겉옷은 가장 추운 시각 기준",
                min, summary.coldest().hour(), coldBand.getRangeText()));
    }
}
