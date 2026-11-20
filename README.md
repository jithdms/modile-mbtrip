# modile-mbtrip

## 디자인 시스템 (브랜드 색 `#A7E7F5`, 심플 톤)

원칙: **그림자 없이 흰 면 + 얇은 회색 선(1dp)**, 단색(그라데이션 없음), **이모지 대신 그림 아이콘**, 움직임은 짧고 은은하게.

| 위치 | 내용 |
| --- | --- |
| `ui/theme/Color.kt` | 팔레트 `Aqua50`~`Aqua800`, `Ink`(글자), `Muted`, `Line`(테두리), `Background`(화면 바탕), `Mint`, `Coral` … |
| `ui/theme/Theme.kt` · `Type.kt` | Material3 색 · 글꼴 크기 · 모서리 |
| `ui/design/Motion.kt` | `bouncyClick`(누르면 살짝 작아짐), `enterFromBelow`/`enterPop`(은은하게 등장), `shake`(틀림), `animatedCount`·`animatedFill`(숫자·막대 차오름) |
| `ui/design/Kit.kt` | 공용 부품 — `MBButton`, `MBCard`, `MBChip`, `ScreenHeader`, `CircleButton`, `FillBar`, `SectionLabel`, `MBIcon`, `mbFieldColors()` |
| `ui/component/MBBottomBar.kt` | 하단 바 (내 여행 · 여행 로그 · 알림 · 친구 · MY) |
| `res/drawable/ic_mb_*.xml` | 앱 아이콘 그림 (24dp 검정 벡터, 쓰는 곳에서 색을 입힘) |
| `res/values/colors.xml` | XML 화면용 같은 팔레트 (`mb_aqua_300`, `mb_line` …) · 버튼 `mb_button_primary/deep/outline` |

새 화면을 만들 때는 `Color(0xFF…)`·이모지를 직접 쓰지 말고 위 이름과 `ic_mb_*` 아이콘을 쓰면 앱 전체 톤이 맞춰집니다.

### 내 여행 목록
`session/MyTrips.kt` — 이 폰에서 만들거나 초대코드로 들어간 여행을 기기에 기억해 두고, 목록을 열 때 Firestore 에서 날짜·목적지를 새로 읽어 맞춥니다.
(Firestore 에서 "내가 들어간 여행 전체"를 바로 찾으려면 `members` 컬렉션 그룹 색인과 보안 규칙이 따로 필요해서 이렇게 했습니다.)

### 계정 · 데이터 저장 (Firebase)
- **로그인**: Firebase Authentication 이메일/비밀번호 (`session/Account.kt`). Console → Authentication → 로그인 방법에서 **이메일/비밀번호**와 **익명**을 켜 주세요.
- **저장되는 곳**
  - `users/{uid}` — 이름 · @아이디 · 이메일 · 한 줄 소개 · 진단 점수 · 프로필 사진(작게 줄인 JPEG, Base64)
  - `users/{uid}/myTrips/{tripId}` — 내가 만들거나 들어간 여행 목록
  - `trips/{tripId}` (+ `members`, `blocks`, `votes`, `comments`, `reviews`) — 여행 방과 일정
- **보안 규칙**: `docs/firestore.rules` 를 Console → Firestore → 규칙에 붙여 넣으세요. (규칙이 막혀 있으면 PERMISSION_DENIED 가 납니다)

### 테스트 계정 (테스트 자료 보기)
`session/TestAccess.kt` 의 `TESTER_EMAILS` 에 있는 이메일(기본: `test@mbtrip.com`)로 가입·로그인하거나,
Firestore `users/{uid}` 문서에 `tester: true` 를 넣은 계정만 아래 테스트 자료를 봅니다.
- 가짜 친구 목록 · 예시 알림 · 예시 여행 로그 · 일정표의 [테스트용 가짜 일정 넣기] · 내 여행의 '체험용 여행'

다른 계정은 실제로 만든 자료만 보입니다 (친구 = 함께 여행한 사람, 알림 = 빈 알림함부터).

### 시흥 장소 · 추천 투어 루틴
- 장소 목록: `siheung/SiheungPlaces.kt` (팀 문서의 명소 50 · 맛집 23 · 카페 23). 고칠 땐 이 파일만 바꾸면 된다.
- 좌표: 문서에 없어서 `PlaceLocator` 가 브이월드 검색(주소 → 이름)으로 처음 한 번 찾고 폰에 저장한다.
  브이월드 인증키에 **검색 API** 가 켜져 있어야 점이 찍힌다.
- 지도: 일정 모달의 작은 지도 · 전체 지도에 색 점(명소 청록 · 맛집 산호 · 카페 갈색). 15단계 이상 확대하면 이름이 보이고, 점을 누르면 그 장소로 골라진다.
- 블록 추천: 일정 모달 '시흥 추천' — 시간대(점심·저녁 = 맛집, 오후 = 카페)와 이전 일정 위치를 보고 가까운 곳부터.
- 투어 루틴: `siheung/TourRoutines.kt`. 방 만들기 아래 카드 → 미리보기 → [이 순서로 돌아다니기] → 여행이 만들어지고 일정표를 처음 열 때 블록(+이동 블록)으로 들어간다.

### 친구 · 실시간
- 친구: `users/{uid}` 문서의 `friends` 칸. 일정표 '친구 초대' 모달에는 친구만 나온다.
- 친구 신청: `trips/_social/friendRequests/{보낸uid}_{받는uid}` (pending → 받은 사람이 알림에서 수락/거절). 수락하면 양쪽 friends 칸에 들어가고, 친구 삭제도 상대 쪽에 반영된다. 테스트 계정은 신청 없이 바로 친구.
- 실시간: 블록 · 동행자 · 여행 기간을 Firestore 실시간 리스너로 듣고, 끊기면 다시 붙는다.
  같은 일정표를 보는 사람은 `trips/{tripId}/presence` 에 남아 화면 아래 "○○ 님이 함께 보는 중" 으로 보인다.
- 규칙: `docs/firestore.rules` 의 friends 부분을 Firebase 콘솔 규칙에 꼭 다시 게시할 것.

### 글꼴 · 시작 화면
- 글꼴: 프리텐다드 (`res/font/pretendard_*.otf`, 라이선스 `docs/PRETENDARD_LICENSE.txt`). Compose 는 `ui/theme/Type.kt`, XML 화면은 `themes.xml` 에서 적용.
- 랜딩은 이 폰에서 처음 켰을 때만 보인다. 로그인 상태면 바로 방 만들기, 소개를 본 적 있으면 로그인부터. (다시 보려면 앱 데이터 삭제)

### 후기 알림 · 후기 쓰기
- 오는 날이 지난 여행 중 내가 후기를 안 쓴 여행마다 알림 "○○ 여행은 어떠셨나요?" (알림 화면 '후기' 필터). 앱을 켤 때 · 로그인 직후 · 알림 화면을 열 때 확인.
- 알림을 누르면 후기 쓰기(`triplog/ReviewWriteScreen.kt`): 별 1~5 · 제목 · 내용 · 사진 3장 → `trips/{tripId}/reviews/{uid}`. 사진은 720px JPEG 로 줄여 문서 안에 `data:image/jpeg;base64,…` 로 저장(Firebase Storage 없이).
- 마이페이지 달력에서 후기 없는 지난 여행을 눌러도 후기 쓰기로 간다.
