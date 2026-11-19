package readygo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 기상청 단기예보 API에서 오늘 하루치 날씨를 가져온다. */
public class ApiWeatherSource implements WeatherSource {

    private static final String ENDPOINT = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Region region;
    private final String apiKey;
    private final LocalDate today;

    public ApiWeatherSource(Region region, String apiKey, LocalDate today) {
        this.region = region;
        this.apiKey = apiKey;
        this.today = today;
    }

    @Override
    public DailyWeather load() throws WeatherLoadException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherLoadException("API 키가 없습니다. 환경변수 KMA_API_KEY를 설정해 주세요.");
        }
        byte[] body = fetch();
        Map<Integer, HourlyWeather> hours = parse(new ByteArrayInputStream(body), today.format(YMD));
        return new DailyWeather(region.label() + " (기상청 예보)", hours);
    }

    /** 전날 23시 발표 예보를 요청한다 (오늘 00~23시 전체 포함). */
    private byte[] fetch() throws WeatherLoadException {
        String url = ENDPOINT
                + "?serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&pageNo=1&numOfRows=1000&dataType=XML"
                + "&base_date=" + today.minusDays(1).format(YMD)
                + "&base_time=2300"
                + "&nx=" + region.nx() + "&ny=" + region.ny();
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) {
                throw new WeatherLoadException("기상청 서버 응답 오류 (HTTP " + res.statusCode() + ")");
            }
            return res.body();
        } catch (IOException e) {
            throw new WeatherLoadException("기상청 서버에 연결하지 못했습니다: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherLoadException("요청이 중단되었습니다.", e);
        }
    }

    /** XML 응답 → 시각별 날씨. 네 항목(TMP·POP·PTY·REH)이 모두 있는 시각만 만든다. */
    static Map<Integer, HourlyWeather> parse(InputStream xml, String targetDate) throws WeatherLoadException {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
        } catch (Exception e) {
            throw new WeatherLoadException("응답을 해석하지 못했습니다(XML 아님).", e);
        }

        // 키 오류 등은 HTTP 200이어도 resultCode/returnAuthMsg로 온다.
        String code = text(doc.getDocumentElement(), "resultCode");
        if (code == null) {
            String auth = text(doc.getDocumentElement(), "returnAuthMsg");
            throw new WeatherLoadException("API 오류: " + (auth != null ? auth : "알 수 없는 응답"));
        }
        if (!code.equals("00")) {
            throw new WeatherLoadException("API 오류 " + code + ": " + text(doc.getDocumentElement(), "resultMsg"));
        }

        Map<Integer, Map<String, Integer>> raw = new HashMap<>();
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            if (!targetDate.equals(text(item, "fcstDate")))
                continue;
            String cat = text(item, "category");
            if (!cat.equals("TMP") && !cat.equals("POP") && !cat.equals("PTY") && !cat.equals("REH"))
                continue;
            int hour = Integer.parseInt(text(item, "fcstTime").substring(0, 2));
            int value = (int) Math.round(Double.parseDouble(text(item, "fcstValue")));
            raw.computeIfAbsent(hour, h -> new HashMap<>()).put(cat, value);
        }

        Map<Integer, HourlyWeather> result = new TreeMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> e : raw.entrySet()) {
            Map<String, Integer> v = e.getValue();
            if (v.keySet().containsAll(List.of("TMP", "POP", "PTY", "REH"))) {
                result.put(e.getKey(), new HourlyWeather(e.getKey(),
                        v.get("TMP"), v.get("POP"), v.get("PTY"), v.get("REH")));
            }
        }
        if (result.isEmpty()) {
            throw new WeatherLoadException("오늘(" + targetDate + ") 예보 데이터가 없습니다.");
        }
        return result;
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? null : list.item(0).getTextContent().trim();
    }
}