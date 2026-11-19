package readygo;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 기상청 API 키를 찾는다.
 * <ol>
 *   <li>환경변수 KMA_API_KEY</li>
 *   <li>실행 위치의 api.properties 파일 (.gitignore로 커밋 제외)</li>
 * </ol>
 * 둘 다 없으면 null을 돌려준다. 안내 메시지는 ApiWeatherSource가 처리한다.
 */
public final class ApiKeyLoader {

    static final String KEY_NAME = "KMA_API_KEY";
    static final Path DEFAULT_FILE = Path.of("api.properties");

    private ApiKeyLoader() {
    }

    public static String load() {
        return load(System.getenv(KEY_NAME), DEFAULT_FILE);
    }

    /** 환경변수 값과 파일 경로를 받아 키를 찾는다 (load()가 사용). */
    static String load(String envValue, Path file) {
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Properties p = new Properties();
                p.load(r);
                String v = p.getProperty(KEY_NAME);
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            } catch (IOException e) {
                System.out.println("[경고] " + file + " 파일을 읽지 못했습니다: " + e.getMessage());
            }
        }
        return null;
    }
}
