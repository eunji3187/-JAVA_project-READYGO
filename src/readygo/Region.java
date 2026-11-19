package readygo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 지역 이름과 기상청 격자 좌표(nx, ny).
 * 격자값 출처: 기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx
 * - 시·도는 엑셀의 1단계 행(2·3단계가 빈 행) 값을 쓴다.
 * - 시는 시청이 있는 구(區)의 값을 쓴다.
 */
public enum Region {
    // 특별시·광역시·특별자치시
    SEOUL("서울", 60, 127),
    BUSAN("부산", 98, 76),
    DAEGU("대구", 89, 90),
    INCHEON("인천", 55, 124),
    GWANGJU("광주", 59, 74),          // 전남광주통합특별시 서구 (옛 광주광역시청)
    DAEJEON("대전", 67, 100),
    ULSAN("울산", 102, 84),
    SEJONG("세종", 66, 103),

    // 경기
    GYEONGGI("경기", 60, 120),
    SUWON("수원", 61, 121),           // 팔달구
    SEONGNAM("성남", 63, 124),        // 중원구
    GOYANG("고양", 57, 128),          // 덕양구
    YONGIN("용인", 64, 119),          // 처인구
    GYEONGGI_GWANGJU("경기광주", 65, 123),

    // 강원
    GANGWON("강원", 73, 134),         // 도청: 춘천
    CHUNCHEON("춘천", 73, 134),
    WONJU("원주", 76, 122),
    GANGNEUNG("강릉", 92, 131),

    // 충청
    CHUNGBUK("충북", 69, 107, "충청북"),
    CHEONGJU("청주", 69, 106),        // 상당구
    CHUNGNAM("충남", 55, 107, "충청남"),
    CHEONAN("천안", 63, 110),         // 동남구

    // 전라 (2026 격자표: 전남과 광주가 '전남광주통합특별시'로 묶임)
    JEONBUK("전북", 63, 89, "전라북", "전북특별자치"),
    JEONJU("전주", 63, 89),
    JEONNAM("전남", 51, 67, "전라남", "전남광주통합"),
    MOKPO("목포", 50, 67),
    YEOSU("여수", 73, 66),
    SUNCHEON("순천", 70, 70),

    // 경상
    GYEONGBUK("경북", 87, 106, "경상북"),
    POHANG("포항", 102, 94),          // 남구
    ANDONG("안동", 91, 106),
    GYEONGNAM("경남", 91, 77, "경상남"),
    CHANGWON("창원", 90, 77),         // 의창구

    // 제주
    JEJU("제주", 52, 38),
    SEOGWIPO("서귀포", 53, 33);

    private final String label;
    private final int nx, ny;
    private final String[] aliases;

    Region(String label, int nx, int ny, String... aliases) {
        this.label = label;
        this.nx = nx;
        this.ny = ny;
        this.aliases = aliases;
    }

    public String label() {
        return label;
    }

    public int nx() {
        return nx;
    }

    public int ny() {
        return ny;
    }

    /** "서울특별시", "수원시", " 경기도 ", "충청북도" 등을 받아 찾는다. */
    public static Optional<Region> find(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String key = normalize(input);
        for (Region r : values()) {
            if (r.label.equals(key)) {
                return Optional.of(r);
            }
            for (String alias : r.aliases) {
                if (alias.equals(key)) {
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    /** 공백을 지우고 끝의 행정구역 단위(특별시·광역시·도·시 등)를 한 번 떼어 낸다. */
    static String normalize(String s) {
        String t = s.replace(" ", "").trim();
        for (String suffix : new String[] { "특별자치도", "특별자치시", "특별시", "광역시", "도", "시", "군" }) {
            if (t.endsWith(suffix) && t.length() > suffix.length()) {
                return t.substring(0, t.length() - suffix.length());
            }
        }
        return t;
    }

    public static List<String> labels() {
        List<String> list = new ArrayList<>();
        for (Region r : values()) {
            list.add(r.label);
        }
        return list;
    }
}
