package readygo;

/**
 * 한 시간의 날씨. 예보의 한 시각이 HourlyWeather 하나가 된다.
 *
 * @param hour     시각 (0~23)
 * @param temp     기온 (°C)
 * @param pop      강수확률 (0~100 %)
 * @param pty      강수형태 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기 — 기상청 단기예보 코드)
 * @param humidity 습도 (0~100 %)
 */
public record HourlyWeather(int hour, int temp, int pop, int pty, int humidity) {

    /** 생성자 검사. 공급원이 먼저 거르므로 이중 안전장치다. */
    public HourlyWeather {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("시각은 0~23이어야 합니다: " + hour);
        }
        if (pop < 0 || pop > 100) {
            throw new IllegalArgumentException("강수확률은 0~100이어야 합니다: " + pop);
        }
        if (pty < 0 || pty > 4) {
            throw new IllegalArgumentException("강수형태는 0~4여야 합니다: " + pty);
        }
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("습도는 0~100이어야 합니다: " + humidity);
        }
    }

    /** 강수형태가 1(비), 2(비/눈), 4(소나기)면 true */
    public boolean isRainType() {
        return pty == 1 || pty == 2 || pty == 4;
    }

    /** 강수형태가 2(비/눈), 3(눈)이면 true */
    public boolean isSnowType() {
        return pty == 2 || pty == 3;
    }

    /**
     * 기상청 여름철 체감온도 산출식 (소수 첫째 자리로 반올림).
     * <pre>
     * Tw = Ta·atan[0.151977(RH+8.313659)^½] + atan(Ta+RH) − atan(RH−1.67633)
     *      + 0.00391838·RH^1.5·atan(0.023101·RH) − 4.686035      (Stull 습구온도)
     * 체감 = −0.2442 + 0.55399·Tw + 0.45535·Ta − 0.0022·Tw² + 0.00278·Tw·Ta + 3.0
     * </pre>
     * 예: 35°C, 습도 56% → 35.1°C
     */
    public double feelsLike() {
        double ta = temp;
        double rh = humidity;
        double tw = ta * Math.atan(0.151977 * Math.sqrt(rh + 8.313659))
                + Math.atan(ta + rh)
                - Math.atan(rh - 1.67633)
                + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
                - 4.686035;
        double feels = -0.2442 + 0.55399 * tw + 0.45535 * ta
                - 0.0022 * tw * tw + 0.00278 * tw * ta + 3.0;
        return Math.round(feels * 10) / 10.0;
    }

    /** 강수형태 숫자 → 이름 */
    public static String ptyName(int pty) {
        switch (pty) {
            case 0: return "없음";
            case 1: return "비";
            case 2: return "비/눈";
            case 3: return "눈";
            case 4: return "소나기";
            default: return "알 수 없음(" + pty + ")";
        }
    }
}
