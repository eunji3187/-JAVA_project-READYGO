package readygo;

/** 모든 정수 기온을 지원하는 옷차림 기준표. */
public enum ClothingBand {
    FREEZING(Integer.MIN_VALUE, 2, "3°C 미만",
            "발열 내의·두꺼운 니트",
            "두꺼운 패딩·방한 코트",
            "기모 바지·두꺼운 양말·방한화"),
    COLD(3, 10, "3°C 이상 11°C 미만",
            "기모 안감이 있는 니트·긴팔",
            "코트·경량 패딩·두꺼운 재킷",
            "긴바지·양말·로퍼·앵클부츠"),
    COOL(11, 14, "11°C 이상 15°C 미만",
            "셔츠·얇은 니트",
            "트렌치코트·블레이저·데님 재킷",
            "청바지·면바지·운동화"),
    MILD(15, 19, "15°C 이상 19°C 이하",
            "긴팔 티셔츠·얇은 니트",
            "가디건·얇은 바람막이",
            "슬랙스·청바지·로퍼"),
    WARM(20, 22, "20°C 이상 23°C 미만",
            "얇은 긴팔·반팔 티셔츠", "얇은 가디건",
            "면바지·얇은 청바지·운동화"),
    HOT(23, 27, "23°C 이상 28°C 미만",
            "반팔 티셔츠·얇은 셔츠", "별도 겉옷 불필요",
            "얇은 면바지·반바지·통풍이 좋은 운동화"),
    VERY_HOT(28, Integer.MAX_VALUE, "28°C 이상",
            "통풍이 좋은 반팔·린넨 셔츠", "별도 겉옷 불필요",
            "반바지·린넨 바지·샌들");

    private final int min;            // 포함
    private final int max;            // 포함
    private final String rangeText;
    private final String top;
    private final String outer;
    private final String bottomAndShoes;

    ClothingBand(int min, int max, String rangeText, String top, String outer, String bottomAndShoes) {
        this.min = min;
        this.max = max;
        this.rangeText = rangeText;
        this.top = top;
        this.outer = outer;
        this.bottomAndShoes = bottomAndShoes;
    }

    /** 기온이 속한 구간. 7개 구간이 모든 정수 기온을 빈틈없이 덮는다. */
    public static ClothingBand of(int temp) {
        for (ClothingBand band : values()) {
            if (temp >= band.min && temp <= band.max) {
                return band;
            }
        }
        throw new IllegalStateException("기온 구간을 찾지 못했습니다: " + temp); // 구간이 빈틈없으므로 실행되지 않음
    }

    public String getRangeText() {
        return rangeText;
    }

    public String getTop() {
        return top;
    }

    public String getOuter() {
        return outer;
    }

    public String getBottomAndShoes() {
        return bottomAndShoes;
    }
}
