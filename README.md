# ReadyGo — 날씨 보고 외출 준비하기

기상청 단기예보로 오늘 날씨를 불러와 **외출 시간대**의 기온·강수를 분석하고, 복장과 준비물을 추천하는 Java 콘솔 프로그램입니다.

- **대상 사용자:** 등교·출근 등으로 하루 중 긴 시간 외출하는 사람
- **해결하려는 문제:** 지금 날씨만 보고 나가면 오후 비나 저녁 기온 하강을 놓치기 쉽다. 외출~귀가 구간 전체를 보고 옷차림과 준비물을 한 번에 정리해 준다.

### 요구 환경

- JDK 17 이상 (개발·시험: OpenJDK 25)
- 인터넷 연결, 공공데이터포털 기상청 API 인증키
- 외부 라이브러리 없음

### 1) API 키 발급

1. [공공데이터포털](https://www.data.go.kr)에서 **기상청_단기예보 조회서비스** 활용 신청
   [www.data.go.kr/data/15084084/openapi.do](https://www.data.go.kr/data/15084084/openapi.do)
2. 마이페이지에서 **일반 인증키(Decoding)** 복사

### 2) API 키 설정 (둘 중 하나)

- `api.properties.example`을 복사해 `api.properties`로 이름을 바꾸고 `KMA_API_KEY=` 뒤에 키 입력
- 또는 환경변수 `KMA_API_KEY` 설정 (파일보다 우선)

> `api.properties`는 `.gitignore`에 등록되어 저장소에 올라가지 않습니다.

### 3) 실행

| 명령                                                | 동작                                                             |
| --------------------------------------------------- | ---------------------------------------------------------------- |
| `run.bat` (Windows) / `./run.sh` (macOS·Linux) | 서울 날씨로 실행                                                 |
| `run.bat 부산` / `./run.sh 부산`                | 지정한 지역 날씨로 실행 (`부산광역시`, `수원시`처럼 써도 됨) |
| `tests                                              |                                                                  |
| un_tests.bat`/`./tests/run_tests.sh`              | 자동 시험 실행                                                   |

스크립트가 컴파일(`out/`)과 실행을 함께 합니다. VS Code에서는 F5(`.vscode/launch.json`)로도 실행할 수 있습니다.

### 사용 순서

1. 오늘 24시간 날씨 표가 출력됩니다.
2. 외출 시각, 귀가 시각(0~23, 귀가 시각 포함)을 입력합니다.
3. 외출 목적(공부·출근·쇼핑)을 고릅니다. 외출·귀가 시각이 같으면 '잠깐 외출'을 선택할 수 있습니다.
4. 외출 구간 요약과 복장·준비물이 출력됩니다.
5. `종료하시겠습니까? (y/n)` — `n`이면 추천 이유와 안내를 보여준 뒤 종료합니다.

잘못된 입력(글자, 범위 밖 숫자, 귀가가 외출보다 빠름)은 다시 묻고, 날씨를 불러오지 못하거나 외출 구간에 데이터가 빈 시각이 있으면 이유를 알려주고 종료합니다.

## 폴더 구조

```
readygo/
├── src/readygo/            # 프로그램 소스 (22개 파일)
├── tests/
│   ├── ReadyGoTests.java   # 자동 시험 (단위 U-01~14, 콘솔 C-01~12)
│   ├── readygo/            # 시험 전용: CsvWeatherSource, TestMain
│   ├── data/               # 시험용 가상 날씨 CSV (시나리오 A~D)
│   ├── fixtures/           # 형식 오류 CSV
│   └── results/            # 시험 결과 기록 (C-xx.txt, summary.txt)
├── api.properties.example  # API 키 설정 예시
├── run.bat / run.sh        # 컴파일 + 실행
└── README.md
```

# 작동 원리

## 1. 전체 흐름

```
Main.main(args)
  │  지역 이름 확인 (없으면 서울)
  ▼
ApiWeatherSource  (Region 격자 좌표 + ApiKeyLoader 키)
  │  WeatherSource 인터페이스로 전달
  ▼
┌─ ConsoleUI.run(source) ─────────────────────────────────────────┐
│                                                                 │
│  ① source.load()                                                │
│     → DailyWeather (시각별 HourlyWeather 최대 24개)  ┄┄▶ WeatherLoadException → 종료
│  ② printToday() · readPlan()                                    │
│     오늘 날씨 표 출력 → 출발·귀가 시각, 목적 입력               │
│  ③ WeatherAnalyzer.analyze()                                    │
│     외출 구간만 잘라 → WeatherSummary         ┄┄▶ MissingWeatherDataException
│  ④ Recommender.recommend()                                      │
│     규칙 4개가 차례로 Recommendation을 채움                     │
│  ⑤ printSummary() · printRecommendation()                       │
│     요약 + 복장·준비물 출력                                      │
│  ⑥ askExit()  종료하시겠습니까? (y/n)                            │
│     y → 바로 종료                                               │
│     n → printDetails() 이유·안내 출력 후 종료                    │
└─────────────────────────────────────────────────────────────────┘
```

> ┄┄▶ 는 예외 경로입니다. 모든 오류는 `ConsoleUI.run()` 한 곳에서 잡아 안내 메시지를 출력하고 안전하게 종료합니다.

## 2. 날씨 불러오기 — WeatherSource 인터페이스

콘솔은 `WeatherSource.load()`라는 약속만 알기 때문에, 날씨가 어디서 오는지와 상관없이 이후 코드는 바뀌지 않습니다.

```
        «interface»
   WeatherSource · load()
            △
            │
     ApiWeatherSource
```

**ApiWeatherSource**

1. `Region.find("서울")` → 격자 좌표 nx, ny
2. `ApiKeyLoader.load()` → 인증키
3. `fetch()`: HTTP GET (전날 23시 발표 예보 → 오늘 00~23시)
4. `parse()`: XML에서 오늘 날짜의 TMP·POP·PTY·REH만 골라 시각별로 묶음
5. `resultCode ≠ 00`, 연결 실패, 키 없음 → `WeatherLoadException`
6. → `DailyWeather`

| API 항목 | 의미                                             | HourlyWeather 필드 |
| -------- | ------------------------------------------------ | ------------------ |
| TMP      | 기온 (°C)                                       | temp               |
| POP      | 강수확률 (%)                                     | pop                |
| PTY      | 강수형태 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기) | pty                |
| REH      | 습도 (%)                                         | humidity           |

**Region** — 지역 이름 ↔ 기상청 격자 좌표. `서울특별시`, `수원시`, `충청북도`처럼 행정구역 단위를 붙여도 찾습니다.
지원 지역: 서울, 부산, 대구, 인천, 광주, 대전, 울산, 세종, 경기, 수원, 성남, 고양, 용인, 경기광주,
강원, 춘천, 원주, 강릉, 충북, 청주, 충남, 천안, 전북, 전주, 전남, 목포, 여수, 순천,
경북, 포항, 안동, 경남, 창원, 제주, 서귀포

## 3. 분석 — WeatherAnalyzer

```
DailyWeather (00~23시)
0시 ├──────────[ 외출 구간 (예: 09시 ~ 21시, 귀가 시각 포함) ]──────┤ 23시
                         │
                         ▼ weather.range(start, end)
                           — 빈 시각이 있으면 MissingWeatherDataException
```

한 번의 반복으로 찾는 값 → `WeatherSummary`

- `coldest` / `warmest` : 최저·최고 기온 시각 (같으면 이른 시각)
- `maxPop` : 최대 강수확률
- `precipHours` : 강수확률 ≥ 50% 인 시각 목록
- `maxFeelsLike` : 최고 체감온도 (기온 + 습도로 계산하는 기상청 여름철 식)
- 일교차 = 최고 − 최저
- 비·눈 예상 : PTY로 구분

## 4. 추천 — Recommender와 규칙(Rule)

빈 `Recommendation`을 하나 만들고, 규칙 4개가 순서대로 `apply(summary, plan, rec)`를 호출해 내용을 채웁니다.
새 규칙은 `Rule`을 구현해 목록에 추가하기만 하면 됩니다.

```
WeatherSummary + OutingPlan
   ▼
OutfitRule ─▶ PurposeItemRule ─▶ PrecipitationRule ─▶ HeatRule
   ▼
Recommendation
```

| 규칙              | 판단                                                                            |
| ----------------- | ------------------------------------------------------------------------------- |
| OutfitRule        | 최고 기온 → 상의·하의·신발, 최저 기온 → 겉옷 (ClothingBand 7구간)           |
| PurposeItemRule   | 공통: 휴대폰·지갑·열쇠 + 목적별 준비물 (공부·출근·쇼핑, 잠깐 외출은 공통만) |
| PrecipitationRule | 강수확률 ≥ 50% → 우산, 비 → 장화, 눈 → 특보 확인 안내                       |
| HeatRule          | 최고 체감온도 ≥ 33°C → 물병                                                  |

**Recommendation (결과 그릇)**

- `top` / `outer` / `bottomAndShoes` : 복장
- `items` : 준비물 (LinkedHashSet → 중복 제거, 넣은 순서 유지)
- `reasons` : 판단 근거 (n 입력 시 출력)
- `notices` : 사용자가 따로 확인할 주의사항 (예: 눈 예보 시 기상특보 확인 안내, 있을 때만 출력)

### 실행 화면 예 (09~21시, 공부)

```
=== 외출 구간 요약: 09시 ~ 21시 (13시간, 공부) ===
  최저 기온     : 9°C (09시)
  최고 기온     : 17°C (14시)
  기온 차       : 8°C
  최대 강수확률 : 70%
  강수 예상     : 15시(비 60%), 16시(비 70%), 17시(비 60%)
  최고 체감온도 : 18.2°C (15시)

=== 추천 결과 ===
[복장]
  상의      : 긴팔 티셔츠·얇은 니트
  겉옷      : 코트·경량 패딩·두꺼운 재킷
  하의·신발 : 슬랙스·청바지·로퍼
[준비물] 8개
  휴대폰, 지갑, 열쇠, 노트북, 충전기, 필기구, 우산, 장화

종료하시겠습니까? (y/n): n

[이유]
  1. 최고 기온 17°C(14시)가 15°C 이상 19°C 이하 구간 → 상의·하의는 이 구간 기준
  2. 최저 기온 9°C(09시)가 3°C 이상 11°C 미만 구간 → 겉옷은 가장 추운 시각 기준
  3. 목적 '공부' → 공통 준비물(휴대폰·지갑·열쇠) + 목적 준비물(노트북·충전기·필기구)
  4. 15·16·17시 강수확률 50% 이상(최대 70%) → 우산
  5. 15·16·17시 비 예보 → 장화

프로그램을 종료합니다.
```

## 5. 클래스별 역할 한눈에 보기

| 구분          | 클래스                      | 역할                                                     |
| ------------- | --------------------------- | -------------------------------------------------------- |
| 시작·화면    | Main                        | 지역을 확인하고 ApiWeatherSource를 만들어 ConsoleUI 실행 |
|               | ConsoleUI                   | 전체 진행, 입력(Scanner)·출력, 예외 처리                |
| 날씨 불러오기 | WeatherSource «interface» | load() 약속                                              |
|               | ApiWeatherSource            | 기상청 단기예보 API 호출(fetch)과 XML 해석(parse)        |
|               | Region «enum»             | 지역 이름 ↔ 격자 좌표(nx, ny), 별칭 검색                |
|               | ApiKeyLoader                | 인증키 찾기                                              |
| 데이터        | HourlyWeather «record»    | 한 시각의 기온·강수확률·강수형태·습도, 체감온도 계산  |
|               | DailyWeather                | 지역 이름 + 시각별 날씨 묶음, 구간 잘라내기              |
|               | OutingPlan «record»       | 출발·귀가 시각, 목적                                    |
|               | Purpose «enum»            | 공부·출근·쇼핑·잠깐 외출과 목적별 준비물              |
| 분석          | WeatherAnalyzer             | 외출 구간 분석 → WeatherSummary                         |
|               | WeatherSummary «record»   | 최저·최고·강수·체감온도 요약                          |
| 추천          | Recommender                 | 규칙 목록을 차례로 적용                                  |
|               | Rule «interface»          | apply(summary, plan, rec) 약속                           |
|               | OutfitRule                  | 기온 구간별 복장                                         |
|               | PurposeItemRule             | 공통·목적별 준비물                                      |
|               | PrecipitationRule           | 우산·장화, 눈 안내                                      |
|               | HeatRule                    | 더위 시 물병                                             |
|               | ClothingBand «enum»       | 7개 기온 구간과 구간별 옷차림                            |
| 결과          | Recommendation              | 복장·준비물·근거·안내를 담는 그릇                     |
| 예외          | WeatherLoadException        | 날씨를 불러오지 못함 (연결·응답·키 오류)               |
|               | MissingWeatherDataException | 외출 구간에 데이터 없는 시각이 있음                      |

## 6. 시험 방법

자동 시험은 실제 API 대신 `tests/data`의 **고정된 가상 날씨(CSV)**를 사용합니다. 그래서 날씨가 매일 바뀌어도 같은 결과로 추천 규칙을 검증할 수 있습니다.

- **단위 시험 (U-01~14):** 체감온도 계산, 기온 구간 경계, 분석 값, 추천 결과 등을 클래스를 직접 호출해 확인
- **콘솔 시험 (C-01~12):** 프로그램을 실행하고 입력을 넣어 화면 출력을 확인 (잘못된 입력, 데이터 누락, 입력 종료 등)
- 결과는 `tests/results/`에 저장됩니다. 최근 결과: **26개 항목 통과, 실패 0**

## 7. 한계

- 오늘(00~23시) 하루만 지원하며, 자정을 넘기는 일정은 지원하지 않습니다.
- 추천은 한 번 실행에 한 번 합니다.
- 옷차림 기온 구간과 강수확률 50% 기준은 직접 정한 기준입니다.
- 눈은 적설량(폭설) 기준을 적용하지 않고 안내만 합니다.

> 설계 포인트: **인터페이스로 갈아끼우기**(WeatherSource, Rule) · **단계별 책임 분리**(불러오기 → 분석 → 추천 → 출력) · **예외는 한 곳에서 처리**(ConsoleUI.run)
