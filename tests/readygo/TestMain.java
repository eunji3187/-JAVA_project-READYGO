package readygo;

/**
 * [시험 전용] 콘솔 시험(C-xx) 진입점. 고정된 CSV 날씨로 ConsoleUI를 실행한다.
 * 실제 프로그램 진입점은 {@link Main} (기상청 API 전용).
 */
public class TestMain {

    public static void main(String[] args) {
        new ConsoleUI().run(new CsvWeatherSource(args[0]));
    }
}
