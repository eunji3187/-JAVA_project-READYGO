package readygo;

/**
 * 날씨 데이터를 불러오지 못했을 때 던지는 예외.
 * (API 키 없음, 네트워크·응답 오류, 데이터 형식 오류)
 *
 * <p>checked 예외(Exception 상속)라서 load()를 부르는 쪽은 반드시 try-catch로 처리해야 컴파일된다.
 * 던지는 곳: ApiWeatherSource → 받는 곳: ConsoleUI.run()
 */
public class WeatherLoadException extends Exception {

    private static final long serialVersionUID = 1L;

    public WeatherLoadException(String message) {
        super(message);
    }

    public WeatherLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
