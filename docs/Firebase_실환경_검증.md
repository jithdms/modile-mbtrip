> 최신 상태 (2026-11-17): 2단계 인증 제한이 해제되어 서버 규칙을 수정·게시했다. 내보낸 동행자의 일정 읽기·쓰기·재등록 차단과 정상 멤버 기능의 실서버 재검증이 통과했다. 아래의 과거 실패/MFA 제한 기록보다 [Firebase_규칙_적용_검증.md](Firebase_규칙_적용_검증.md)를 우선한다.

# Firebase 실환경 검증 — 2026-11-17

대상 프로젝트: `mbtrip-590d1`. 로컬 `soyul`, 시작 커밋 `55042d2` 기준. main 변경·커밋·푸시·서버 규칙 배포 없음.

## 실제 서버에서 통과한 항목

Firebase 기본 사용자 세션을 사용하지 않고 이름이 다른 FirebaseApp 2개로 임시 익명 계정 2개를 만들었다. 기본 앱의 여행 캐시와도 분리했다.

- 익명 인증 2계정 성공.
- `users/{uid}` 진단 저장 후 서버 읽기로 점수 확인.
- 실제 여행 문서 및 생성자 멤버 문서 생성 성공.
- 임시 동행자 등록, 실시간 멤버 목록 조회, 동행자의 진단 저장 성공.
- 추천 일정 저장 후 다른 사용자 인스턴스에서 같은 블록 수신 성공.
- 저장소 재생성 및 기존 여행 다시 열기로 캐시 복원·서버 재조회 확인.
- 동행자 계정으로 일정 수정 성공.
- 오래된 화면에서 덮어쓰기 거부 확인.
- 같은 버전의 일정을 두 사용자로 동시에 수정하면 한 요청만 성공함을 확인.
- 앱 저장소 메서드의 생성자 전용 내보내기 검사, 실제 멤버 삭제, 삭제 후 앱 재진입 거부 확인.

위 확인은 한 가상 기기 안의 독립 Firebase SDK 인스턴스 2개로 수행했다. 서로 다른 물리 기기, 이메일 로그인/초대 UI, 완전한 프로세스 종료 후 재로그인까지 확인한 것은 아니다.

## 실패한 서버 접근 제어

임시 동행자를 멤버 문서에서 삭제한 뒤, 해당 계정으로 앱 저장소의 권한 검사를 거치지 않고 Android Firebase SDK에서 직접 요청했다.

| 요청 | 기대 결과 | 실제 결과 |
|---|---|---|
| 해당 여행 blocks 서버 조회 (`Source.SERVER`) | PERMISSION_DENIED | 조회 허용 |
| 해당 여행 blocks 새 검증 블록 쓰기 | PERMISSION_DENIED | 쓰기 허용 |

로그: `SERVER_ACCESS removedMemberReadDenied=false removedMemberWriteDenied=false`.

따라서 앱의 목록 삭제/화면 차단과 서버의 접근 권한 회수는 현재 다르다. 실서버 권한 검증은 실패 상태로 유지한다. 테스트의 실패 조건을 완화하지 않았다. 실제 배포 규칙 본문에는 접근하지 못했으므로 특정 규칙 문장을 원인으로 단정하지 않는다.

두 실서버 실행 모두 임시 여행의 blocks/members/여행 문서, 임시 사용자 프로필 및 익명 Auth 계정 정리가 성공했다(`cleanupErrors=[]`). 기존 사용자 문서는 테스트 대상으로 사용하지 않았다.

## 적용 전 수정안

`firestore-members-access.fragment.rules`에 B 담당 경로의 접근 제어 조각을 준비했다. **서버 미적용, 규칙 엔진 문법/호환성 미검증 초안이다.** 전체 규칙을 이 파일로 교체해서는 안 된다.

- trips 조회는 현재 멤버/생성자로 제한.
- blocks 조회·쓰기 권한은 서버에서도 현재 멤버 여부로 검사.
- members 생성/내보내기는 생성자 권한으로 제한. 자기 점수 갱신은 현재 멤버인 자기 문서에만 허용.
- 여행 최초 생성 batch는 `getAfter()`로 생성자 멤버 등록을 검사.
- 기존 사용자 조회/초대·투표·댓글 등의 팀 경로와 현재 배포 규칙을 함께 검토해 병합.
- 중복되는 광범위 allow가 남아 있으면 제한이 무력화될 수 있으므로 실제 전체 규칙을 확인.
- 구형 여행의 ownerId 유무, 서버 필드 검증, 일정 버전 동시 갱신 규칙도 적용 전 검토.

공식 참고: https://firebase.google.com/docs/firestore/security/rules-conditions

## 현재 콘솔 접근 제한

Firebase Console이 `https://console.firebase.google.com/mfa`로 이동하며 Google 계정에 2단계 인증(MFA) 설정을 요구한다. 해당 계정 설정은 사용자 조작이 필요하다. 현재 규칙 조회·수정·배포는 하지 않았다.

사용자가 2단계 인증을 완료한 뒤 프로젝트의 Firestore Rules 화면에 접근할 수 있으면, 현재 규칙을 읽고 팀 기능을 유지하는 수정본을 작성·규칙 테스트·적용 후 이 실서버 접근 제어 테스트를 다시 실행해야 한다.

## 재실행 명령

일반 회귀 테스트는 실서버 쓰기를 실행하지 않는다. 아래 실서버 검증은 명시적인 옵션이 있어야 실행한다.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug --offline

.\gradlew.bat :app:connectedDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.example.mbtrip.FirebaseLiveIntegrationTest' `
  '-Pandroid.testInstrumentationRunnerArguments.mbtripLiveFirebase=true' --offline
```

실서버 검증에서는 테스트 전용 계정·여행을 새로 생성하고 종료 시 정리한다. 규칙 변경 후에는 정리 권한도 확인해야 한다. 별도 FirebaseApp/캐시를 주입할 수 있도록 TravelRepository 생성자의 선택 인자를 추가했으며, 기본 앱 동작은 유지했다.

## 일반 회귀 검증

변경 후 단위 테스트 70개와 기존 가상 기기 테스트 9개, 디버그 빌드가 통과했다. 명시적 옵션을 주지 않은 일반 실행에서는 실서버 테스트 1개가 의도적으로 건너뛰어진다. 이는 서버 권한 실패가 해결됐다는 뜻이 아니며 실서버 권한 검증은 여전히 실패 상태다.
