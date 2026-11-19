package readygo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 추천 결과 그릇. Recommender가 빈 상태로 만들고 규칙들이 차례로 채운다. */
public class Recommendation {

    private String top;
    private String outer;
    private String bottomAndShoes;
    private final LinkedHashSet<String> items = new LinkedHashSet<>(); // 중복 불가, 넣은 순서 유지
    private final List<String> reasons = new ArrayList<>();
    private final List<String> notices = new ArrayList<>();

    // ---- 복장 ----

    public void setTop(String top) {
        this.top = top;
    }

    public void setOuter(String outer) {
        this.outer = outer;
    }

    public void setBottomAndShoes(String bottomAndShoes) {
        this.bottomAndShoes = bottomAndShoes;
    }

    /** 상의 (정하지 못했으면 빈 Optional → 화면에 '-') */
    public Optional<String> getTop() {
        return Optional.ofNullable(top);
    }

    public Optional<String> getOuter() {
        return Optional.ofNullable(outer);
    }

    public Optional<String> getBottomAndShoes() {
        return Optional.ofNullable(bottomAndShoes);
    }

    // ---- 준비물·이유·안내 ----

    /**
     * 준비물 추가.
     *
     * @return 새로 들어갔으면 true, 이미 있었으면 false (중복 여부를 이유에 남기는 데 쓴다)
     */
    public boolean addItem(String item) {
        return items.add(item);
    }

    public void addReason(String reason) {
        reasons.add(reason);
    }

    public void addNotice(String notice) {
        notices.add(notice);
    }

    /** 준비물 (넣은 순서, 수정 불가) */
    public Set<String> getItems() {
        return Collections.unmodifiableSet(items);
    }

    /** 추천 이유 (수정 불가) */
    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    /** 사용자에게 알릴 안내 (수정 불가) */
    public List<String> getNotices() {
        return Collections.unmodifiableList(notices);
    }
}
