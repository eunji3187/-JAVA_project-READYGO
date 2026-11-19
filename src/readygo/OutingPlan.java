package readygo;

import java.util.Objects;

/**
 * 외출 일정. 귀가 시각(end)도 외출 구간에 포함한다.
 *
 * @param start   외출 시각 (0~23)
 * @param end     귀가 시각 (start~23)
 * @param purpose 외출 목적
 */
public record OutingPlan(int start, int end, Purpose purpose) {

    /** 생성자 검사. 콘솔에서 이미 거르므로 이중 안전장치다. */
    public OutingPlan {
        Objects.requireNonNull(purpose, "purpose");
        if (start < 0 || start > 23 || end < 0 || end > 23) {
            throw new IllegalArgumentException("시각은 0~23이어야 합니다: " + start + "~" + end);
        }
        if (start > end) {
            throw new IllegalArgumentException("귀가 시각이 외출 시각보다 빠릅니다: " + start + "~" + end);
        }
    }

    /** 외출 구간 시간 수 (9~21 → 13) */
    public int hours() {
        return end - start + 1;
    }
}
