# MBTrip 1주차 통합 가이드 (10/11)

받은 브랜치 3개(soeun · dorothy · soyul)를 하나로 합친 결과. jaesung 브랜치는 README뿐이라 합칠 파일 없음.

## 1. 각 브랜치 상태

| 브랜치 | 담당 | 들어 있던 것 | 문제 |
|---|---|---|---|
| soeun | 지소은 | 기본 프로젝트 + 타임라인 **1주차 버전** | v2·v3 미업로드 → 통합본에는 v3 사용 |
| dorothy | 이수진 | 디자인 시스템, 공통 컴포넌트, 랜딩·로그인, 내비게이션 | `MBTrip/` 하위 폴더에 프로젝트 / `.idea` 업로드 / 빌드 도구 버전이 더 높음 / NavGraph에 같은 화면이 두 번 등록 |
| soyul | 신소율 | 성향 진단(코드 + XML 레이아웃), 진단 경로가 추가된 NavGraph | 웹 업로드라 빌드 설정 파일 없음 / `app.iml`(개인 설정) 포함 |

## 2. main에 들어가는 파일 (이 zip 그대로)

| 파일·폴더 | 가져온 곳 | 비고 |
|---|---|---|
| `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew*`, `gradle/wrapper/` | soeun (현재 main) | 빌드 도구 버전 기준 (아래 3번) |
| `gradle/libs.versions.toml` | soeun + **추가** | navigation-compose 2.8.5, material-icons-extended 추가 |
| `app/build.gradle.kts` | soeun + **추가** | 위 2개 의존성 추가 |
| `app/src/main/AndroidManifest.xml` | dorothy | 키보드가 올라올 때 화면 맞춤(adjustResize) |
| `MainActivity.kt` | dorothy | 내비게이션(MBTripNavGraph)으로 시작 |
| `navigation/NavGraph.kt` | soyul 버전 + **일정표 경로 추가** | 중복 등록 정리된 버전 + `timeline` 경로 |
| `ui/theme/` (Color·Theme·Type) | dorothy | 디자인 시스템 (메인 색 #A9D7F5) |
| `ui/component/` (MBTripComponents, TripExampleScreen) | dorothy | 공통 버튼·카드·상단바, 방 만들기 예시 |
| `ui/screens/` (LandingScreens, LoginScreen) | dorothy | 랜딩, 로그인·회원가입 |
| `diagnosis/` (Logic, Screen, View) | soyul | 성향 진단 10문항 |
| `res/layout/diagnosis_screen.xml`, `item_diagnosis_question.xml` | soyul | |
| `res/drawable/diagnosis_*.xml` (3개) | soyul | |
| `timeline/` (8개 파일) | soeun **v3** | 일정표 |
| `test/.../timeline/FakeBlocksTest.kt` | soeun v3 | 테스트 23개 |
| `res/values/`, `res/xml/`, `res/mipmap-*`, 아이콘 | soeun (3개 브랜치 모두 동일) | |

### main에 넣지 않은 것
| 파일 | 이유 |
|---|---|
| `.idea/`, `MBTrip/.idea/`, `app/app.iml` | 개인 Android Studio 설정 |
| `MBTrip/` 하위 폴더 구조 | 내용만 최상위 `app/`로 옮김 |
| dorothy `app/src/main/keepRules/rules.keep` | 내용이 주석뿐이고 우리 빌드 도구 버전에서는 `proguard-rules.pro` 사용 |
| dorothy·soyul의 `NavGraph.kt` 원본 | 하나로 합침 |

## 3. 빌드 도구 버전 — 낮은 쪽(soeun) 기준으로 맞춤

| 항목 | soeun (채택) | dorothy |
|---|---|---|
| AGP | 9.0.1 | 9.3.3 |
| Kotlin | 2.0.21 | 2.2.10 |
| Compose BOM | 2024.09.00 | 2026.02.01 |
| compileSdk / targetSdk | 36.1 / 36 | 37 / 37 |
| coreKtx | 1.17.0 | 1.19.1 |
| Gradle | 9.2.1 | 9.5.0 |

이유: 새 버전 Android Studio는 낮은 AGP를 빌드할 수 있지만, 예전 버전 Studio는 높은 AGP를 못 연다. 지소은 PC에서 9.0.1 동작 확인됨. dorothy 코드는 새 버전 전용 기능을 쓰지 않아 낮은 버전에서도 동작할 것으로 예상(추가 검증 필요).
→ 팀 전체가 Android Studio를 최신으로 맞추면 그때 한 번에 올리면 됨.

## 4. 화면 흐름 (1주차 완료 기준: 4명 화면이 앱 하나에서 열림)

```
랜딩(이수진) → 로그인·회원가입(이수진) → 방 만들기 예시(이수진) → 초대 화면(자리)
                                                                ├─ [진단 화면 미리보기] → 성향 진단(신소율)
                                                                └─ [일정표 화면 미리보기] → 일정표(지소은)
최재성: 투표(P7)·회고(P8) 아직 없음 → 일정표의 onVoteClick 연결 지점만 열려 있음
```

## 5. 통합 순서 (지소은)

1. Android Studio에서 main으로 이동 → **Ctrl+T** (최신 받기)
2. 브랜치 목록 → New Branch → **`integrate`**
3. 탐색기에서 `C:\dev\MBTrip-team` 안의 `app`, `gradle` 폴더와 루트 파일들을 이 zip 내용으로 **덮어쓰기**
   (`.git`, `.idea`, `local.properties`는 건드리지 않기)
4. Sync → Clean Project → ▶ Run → 위 4번 흐름대로 화면이 다 열리는지 확인
5. **Ctrl+K** 커밋 (`1주차 통합: 디자인 시스템·랜딩·로그인·진단·타임라인`) → Unversioned 새 파일 모두 체크
6. **Ctrl+Shift+K** → `integrate → origin : integrate`
7. GitHub에서 PR `main ← integrate` → 리뷰 1명 → Merge

## 6. 통합 후 각 멤버 브랜치에서 할 일

| 브랜치 | 할 일 | 충돌이 나면 |
|---|---|---|
| **soeun** (지소은) | main 받아오기 (Merge 'main' into 'soeun') | timeline 파일 충돌 → **main 쪽(Accept Theirs)** 선택 (main에 v3가 들어 있음) |
| **dorothy** (이수진) | main 받아오기 → **`MBTrip/` 폴더와 `.idea` 폴더 삭제** 후 커밋 | 없음 (경로가 달라 겹치지 않음). 이후 작업은 최상위 `app/`에서 |
| **soyul** (신소율) | main 받아오기 → **`app/app.iml` 삭제** 후 커밋 | `NavGraph.kt` 충돌 → **main 쪽** 선택 (일정표 경로 추가된 버전) |
| **jaesung** (최재성) | main 받아오기 | 없음 |

또는 각자 브랜치를 지우고 **main에서 새로 브랜치를 만들어도 된다** (가장 깔끔).

## 7. 앞으로 각자 넣을 파일 (담당 폴더)

| 사람 | 담당 폴더 / 파일 | 공통 파일 수정 시 |
|---|---|---|
| 이수진 | `ui/theme/`, `ui/component/`, `ui/screens/`, `navigation/` | 디자인 시스템·내비게이션 담당이므로 수정 후 디스코드 공지 |
| 신소율 | `diagnosis/`, `res/layout/diagnosis_*`, `res/drawable/diagnosis_*` | — |
| 지소은 | `timeline/`, `test/.../timeline/` | 빌드 설정(`*.gradle.kts`, `libs.versions.toml`) 담당 |
| 최재성 | 새로 `vote/`(P7 협업·투표), `review/`(P8 회고) 만들기 | 일정표 `onVoteClick(block)`에 연결 |

공통 파일(`MainActivity.kt`, `NavGraph.kt`, `*.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`)은 **수정 전에 디스코드에 먼저 공지**.

## 8. 확인이 필요한 점
- `ui/screens/LandingScreens.kt`의 package가 `ui.screen`(s 없음)인데 폴더는 `ui/screens` → 빌드는 되지만 이수진 님과 하나로 맞추기 권장
- `MainActivity.kt`의 `Greeting`, `GreetingPreview`는 안 쓰는 템플릿 코드 → 이수진 님 확인 후 삭제 가능
- 진단 화면은 XML(View) 방식, 나머지는 Compose → 동작에는 문제없음. 디자인 통일은 2주차 이후 논의
- 화면 파일은 작업 환경에서 컴파일할 수 없어 **Run으로 최종 확인 필요**. 로직(진단·타임라인)은 컴파일 + 테스트 23개 통과
