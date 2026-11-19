package readygo;

import java.util.List;

/**
 * 외출 구간 안에 날씨 데이터가 없는 시각이 있을 때 던지는 예외.
 *
 * <p>던지는 곳: DailyWeather.range() → WeatherAnalyzer → 받는 곳: ConsoleUI.run()
 * 메시지 예: "누락: [12]시"
 */
public class MissingWeatherDataException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingWeatherDataException(List<Integer> missingHours) {
        super("누락: " + missingHours + "시");
    }
}
