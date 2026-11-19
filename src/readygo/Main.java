package readygo;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 프로그램 시작점. 날씨는 기상청 단기예보 API에서만 가져온다.
 * <pre>
 * java -cp out readygo.Main             → 서울 (기본 지역)
 * java -cp out readygo.Main 부산        → 지정한 지역
 * </pre>
 * API 키는 환경변수 KMA_API_KEY 또는 api.properties 파일에서 읽는다 ({@link ApiKeyLoader}).
 */
public class Main {

    static final String DEFAULT_REGION = "서울";

    public static void main(String[] args) {
        String regionName = args.length > 0 ? args[0] : DEFAULT_REGION;

        Optional<Region> region = Region.find(regionName);
        if (region.isEmpty()) {
            System.out.println("[오류] 지원하지 않는 지역: " + regionName);
            System.out.println("       지원 지역: " + String.join(", ", Region.labels()));
            return;
        }
        WeatherSource source = new ApiWeatherSource(region.get(), ApiKeyLoader.load(), LocalDate.now());
        new ConsoleUI().run(source);
    }
}
