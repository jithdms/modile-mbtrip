# B 담당 남은 작업 진행·검증

작업일: 2026-11-18. 실제 `C:\Users\ssins\AndroidStudioProjects\modile-mbtrip`의 로컬 soyul에서 진행했다. 이번 추가 변경은 soyul 게시용 커밋에 포함했다. main 및 서버 규칙을 변경하지 않았다.

## 사용자 선택

- 실제 휴대전화 두 대 검증에 앞서 가상 기기로 검증한다.
- 별도 키/결제 설정을 추가하지 않고 구글지도·네이버지도·블로그 후기 확인 방식으로 유지한다.
- 친구 목록의 범위는 기존 합의대로 현재 여행 동행자다.

## 우선순위 1 — 병합 후 검증과 일정 충돌 수정

전체 검사 재실행에서 기존 동시 저장 검사의 실패를 재현했다. 한 사용자만 저장에 성공했으나, 다른 멤버의 오래된 scheduleVersion 요청은 서버의 +1 규칙에서 PERMISSION_DENIED로 거절될 수 있었다. 기존 검사는 최상위/원인 예외에 IllegalArgumentException이 있어야 한다고 검사하여 실패했다.

TravelRepository는 이 경우 같은 인증 UID가 유지되고 서버에서 읽은 현재 멤버가 실제로 존재하며 일정 버전이 증가한 것을 확인한 뒤에만 일정 충돌 안내로 변환한다. 회원 탈퇴/내보내기 등 실제 접근 거부는 그대로 유지한다. 서버 규칙을 완화하지 않았다.

검사는 오류 종류뿐 아니라 서버 일정 버전이 한 번만 증가하고, 실제 저장된 내용이 성공한 사용자 내용과 일치하는지도 확인한다.

## 우선순위 2 — 시흥·거북섬 여행 추천 보완

- 기존 맛집 20곳·카페 20곳을 유지하고 거북섬 맛집 3곳·카페 3곳을 추가했다.
- 관광 8곳·체험 2곳을 추가하여 총 56곳(맛집 23, 카페 23, 관광 8, 체험 2)이다. 거북섬은 총 11곳이다.
- 관광·체험 분류, 지역 선택, 장소 체류시간 한도, 기존 일정에 있는 장소 제외를 연결했다.
- 정기 운영시간이 확인된 박물관/과학관은 해당 시간 안에서만 배치한다. 정기 월요일·1월 1일 휴관은 후보 및 직접 배분에서 제외한다.
- 공휴일의 월요일/화요일 대체 운영, 설·추석, 임시휴관·계절 운영을 자동 조회하는 기능은 없다. 해당 시설은 방문일 운영 정보를 사용자가 확인해야 선택할 수 있다. 정기 휴관 기준을 보수적으로 적용하므로 공휴일 예외는 공식 공지에서 확인한다.
- 서핑·요트는 운영/예약 확인과 예약 시작 시각 입력이 필요하다. 입력한 시각을 다른 빈 시간으로 자동 이동하지 않는다. 앱이 예약을 대신 수행하거나 성공으로 표시하지 않는다.
- 체류시간은 계획용 예상값이다. 웨이브파크 레슨 공식 안내의 85분과 앱의 준비·샤워 포함 120분 계획값을 구분해 표시한다.
- 구글지도/네이버지도/블로그 후기/운영·선정 근거를 열 수 있다. 별점 데이터를 수집하거나 실시간 별점순으로 정렬하지 않는다. 실제 이동 경로와 시간은 지도에서 확인한다.
- 다른 동행자의 새 일정이나 진단 상태가 들어오면 기존 미리보기를 무효화하고 다시 계산하게 한다. 기존 예약을 오래된 추천으로 덮지 않도록 회귀 검사를 추가했다.
- 시간·비율·충돌로 배치하지 못한 선택 장소를 미리보기에 표시한다.

## 자료 확인

확인일: 2026-11-18. 맛집/카페의 이름·권역·주소를 확인하는 선정 자료로 사용했다. 구글/네이버 별점 수치나 영업시간을 임의로 만들지 않았다.

- 연평도꽃게탕보리밥: https://www.diningcode.com/profile.php?rid=ISFr3eYFWUTx
- 노을조개: https://www.diningcode.com/profile.php?rid=FhjkFflBC1jE
- 웰컴사이공: https://www.diningcode.com/profile.php?rid=U3kEP1YqJ2ss
- 해밀화: https://www.diningcode.com/profile.php?rid=ujMepJpFEE7D
- 누군가에게: https://www.diningcode.com/profile.php?rid=uwEJdsju7pfB
- 랑데자뷰: https://www.diningcode.com/profile.php?rid=eSsW9W68keze
- 거북섬 경관브릿지: https://www.siheung.go.kr/cultour/contents.do?mId=0401170000
- 해양생태과학관 정기 운영: https://www.siheung.go.kr/mesm/itc/guide/information?menuId=8
- 오이도박물관: https://www.siheung.go.kr/cultour/contents.do?mId=0401130000
- 오이도: https://www.siheung.go.kr/cultour/contents.do?mId=0401110000
- 갯골생태공원: https://www.siheung.go.kr/cultour/contents.do?mId=0401070000
- 배곧한울공원: https://www.siheung.go.kr/cultour/contents.do?mId=0401100000
- 물왕호수: https://www.siheung.go.kr/cultour/contents.do?mId=0401040000
- 연꽃테마파크·요트 후보(단체 맞춤형 코스 안내로 개별 예약 가능 여부 별도 확인): https://www.siheung.go.kr/citytour/course/custom
- 웨이브파크 운영일/시간: https://www.wavepark.co.kr/operatinghours
- 웨이브파크 레슨 시간: https://www.wavepark.co.kr/surf

Google Places의 상세 정보와 경로 서비스는 별도 API 구성 범위이며 이번 선택에서 제외했다.
https://developers.google.com/maps/documentation/places/web-service/place-details
https://developers.google.com/maps/documentation/routes/usage-and-billing
네이버 지역검색의 응답 필드에 별점은 포함되지 않는다.
https://developers.naver.com/docs/serviceapi/search/local/local.md

## 검증 결과

- 앱/테스트 APK 빌드 성공.
- 단위 테스트 102개 통과.
- 추천·동행자 화면 집중 검사 7개 통과.
- 최종 전체 화면/실서버 검사 29개 통과. 실패/오류/건너뜀 0개. 이메일 가입/재로그인, 초대, 계정 전환, 동행자 권한, 동시 저장 및 추천 화면을 포함했다. 임시 서버 데이터 정리 결과도 cleanupErrors=[]다.
- 실제 휴대전화 두 대의 전체 사용 흐름은 사용자 선택에 따라 후속 검증으로 남긴다.
