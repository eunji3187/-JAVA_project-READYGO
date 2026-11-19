package readygo;

/**
 * 추천 규칙. 요약과 일정을 읽고 결과 그릇(Recommendation)에 내용을 채운다.
 * 새 규칙은 이 인터페이스를 구현해 {@link Recommender}에 등록하면 다른 코드를 고치지 않아도 된다.
 */
public interface Rule {

    void apply(WeatherSummary summary, OutingPlan plan, Recommendation rec);
}
