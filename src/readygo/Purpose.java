package readygo;

import java.util.List;

/**
 * 외출 목적과 목적별 준비물.
 * 공통 준비물(휴대폰·지갑·열쇠)은 {@link PurposeItemRule#COMMON_ITEMS}에 있다.
 */
public enum Purpose {
    STUDY("공부", List.of("노트북", "충전기", "필기구")),
    WORK("출근", List.of("사원증", "보조배터리", "물병")),
    SHOPPING("쇼핑", List.of("에코백", "보조배터리", "멤버십 카드")),
    /** 외출·귀가 시각이 같을 때만 쓰는 목적. 메뉴에는 나오지 않는다. */
    QUICK("잠깐 외출", List.of());

    private final String displayName;
    private final List<String> items;

    Purpose(String displayName, List<String> items) {
        this.displayName = displayName;
        this.items = items;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 목적 준비물 (수정 불가) */
    public List<String> getItems() {
        return items;
    }

    /** 메뉴에 보여줄 목적 3개 (QUICK 제외) */
    public static List<Purpose> selectable() {
        return List.of(STUDY, WORK, SHOPPING);
    }
}
