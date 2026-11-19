package readygo;

/**
 * 날씨 공급원.
 *
 * <p>콘솔은 이 약속만 알고, 날씨가 어디서 오는지 알 필요가 없다.
 * 프로그램은 {@link ApiWeatherSource}(기상청 단기예보)를 쓴다.
 */
public interface WeatherSource {

    /** 하루치 날씨를 불러온다. 실패하면 WeatherLoadException. */
    DailyWeather load() throws WeatherLoadException;
}
