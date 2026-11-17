# Firestore 규칙 적용 및 실서버 재검증

2026-11-17, Firebase `mbtrip-590d1` 기본 데이터베이스. 로컬 프로젝트는 `soyul`이며 main 브랜치의 커밋 `456d10856fcec47250ccde2f520e916278d08a73`은 유지했다. Git 커밋·푸시는 수행하지 않았다.

## 결과

Google 계정 2단계 인증 완료 후 콘솔 접근이 가능해졌다. 실제 배포 규칙에서 trips 하위 전체와 users 문서에 `allow read, write: if true`가 적용되어 있음을 확인했다. 규칙을 교체·게시하고 실제 Firebase 계정 2개로 재검증했다.

| 검증 항목 | 결과 |
|---|---|
| 콘솔 시뮬레이터의 미인증 일정 읽기 | 거부 |
| 진단 저장 및 서버 점수 읽기 | 통과 |
| 여행 + 생성자 멤버 batch 생성 | 통과 |
| 동행자 등록·실시간 목록·진단 갱신 | 통과 |
| 추천 일정 저장 및 다른 사용자 재조회 | 통과 |
| 멤버 투표·댓글 쓰기 및 생성자 조회 | 통과 |
| 저장소 재생성·여행 다시 열기 | 통과 |
| 동행자의 일정 수정 | 통과 |
| 오래된 화면의 덮어쓰기 거부 | 통과 |
| 같은 버전의 동시 편집 | 한 요청만 성공 |
| 일반 동행자의 내보내기 요청 | 거부 |
| 생성자의 동행자 내보내기 | 통과 |
| 내보낸 동행자의 앱 재진입 | 거부 |
| 내보낸 동행자의 SDK 직접 일정 읽기 | PERMISSION_DENIED |
| 내보낸 동행자의 SDK 직접 일정 쓰기 | PERMISSION_DENIED |
| 내보낸 동행자의 자기 멤버 문서 재등록 | PERMISSION_DENIED |
| 임시 여행·투표·댓글·멤버·프로필·Auth 계정 정리 | 성공 |

실서버 통합 테스트 1개 통과, 실패 0, 건너뜀 0. 이 테스트 안에서 위 여러 단계가 실행된다. SDK 인스턴스 2개를 같은 가상 기기에서 실행했으며 서로 다른 물리 기기나 실제 이메일 로그인/초대 UI 검증을 대체하지 않는다.

최종 로그:

```text
SERVER_ACCESS removedMemberReadDenied=true removedMemberWriteDenied=true removedMemberReaddDenied=true
RESULT stage=complete failure=null cleanupErrors=[]
```

## 규칙 파일

- `firebase/firestore.rules`: 게시된 규칙과 같은 접근 조건을 가진 로컬 관리본(주석·서식은 콘솔과 다를 수 있음).
- `firebase/firestore.before-2026-11-17.rules`: 변경 전 콘솔 규칙 백업. 공개 읽기/쓰기를 허용하는 과거 규칙이므로 그대로 다시 적용하지 않는다.
- `docs/firestore-members-access.fragment.rules`: 이전 미적용 조각 초안이다. 현재 적용본은 `firebase/firestore.rules`이다.

새 규칙은 사용자 프로필 검색을 로그인한 사용자에게 허용하고, 프로필 쓰기는 본인 UID로 제한한다. 여행·일정·멤버 조회는 현재 멤버 또는 생성자로 제한한다. 멤버 초대/삭제는 생성자만 수행하며, 일반 멤버는 자기 진단 점수만 갱신한다. 기존 투표/댓글 경로도 멤버에 한해 유지하며 작성자 UID를 검사한다.

## 기존 팀 코드와의 호환 범위

- 현재 soyul의 여행 생성 경로는 ownerId, tripId, scheduleVersion을 기록하며 실서버 생성·저장 테스트를 통과했다.
- 기존 ownerId 없는 방은 멤버의 조회·블록 편집·scheduleVersion 갱신을 허용하는 조건을 유지했다. 다만 실제 기존 방을 임의 변경해 검증하지는 않았다.
- ownerId 없는 방의 동행자 관리 권한은 추측해서 부여하지 않는다. 생성자 확인 후 데이터 이관이 필요하다.
- 다른 브랜치의 구형 방 생성 코드가 ownerId/scheduleVersion을 쓰지 않거나, 참여 전 초대코드로 trips 전체를 조회하는 흐름이라면 이 규칙과 함께 연동을 보완해야 한다. main 코드는 수정하지 않았다. 초대코드 조회/참여는 별도 권한 설계와 검증이 필요하다.
- 규칙이 전체 일정 겹침이나 상태 전이까지 서버에서 검증하는 것은 아니다. 현재 조치는 접근 권한과 일부 식별 필드 보호를 대상으로 한다.

## 남은 작업

실제 로그인/초대 UI와 서로 다른 기기의 최종 동작 확인, 구형 방의 생성자 정보 이관, 영업시간·좌표·이동시간 기반 추천 보완은 별도 단계다. 이전 `Firebase_실환경_검증.md`의 실패/MFA 차단 기록은 당시 이력이며 현재 서버 권한 결과는 이 문서를 따른다.

## 재검증

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.example.mbtrip.FirebaseLiveIntegrationTest' `
  '-Pandroid.testInstrumentationRunnerArguments.mbtripLiveFirebase=true' --offline
```

위 명령은 매번 별도 테스트 계정·여행을 만들고 정리한다. 일반 회귀 테스트는 실서버 옵션을 켜지 않는다. 규칙은 Firebase Console에서 게시했으며 Firebase CLI용 프로젝트 설정/배포 자동화는 추가하지 않았다.
