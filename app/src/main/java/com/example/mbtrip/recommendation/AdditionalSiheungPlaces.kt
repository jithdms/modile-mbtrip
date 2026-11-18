package com.example.mbtrip.recommendation

import java.time.DayOfWeek
import java.time.MonthDay
import java.time.LocalTime

/** Source-checked on 2026-11-18. Editorial candidates, never Google/Naver star rankings. */
internal object AdditionalSiheungPlaces {
    private val island = SiheungArea.GEOBUKSEOM
    private const val cityTour = "https://www.siheung.go.kr/citytour/course/custom"
    val all = listOf(
        SiheungPlace("food21", "연평도꽃게탕보리밥 오이도거북섬점", PlaceCategory.RESTAURANT, island, 21,
            "게장·보리밥 식사 후보", "https://www.diningcode.com/profile.php?rid=ISFr3eYFWUTx",
            address = "경기도 시흥시 거북섬로 104"),
        SiheungPlace("food22", "노을조개 거북섬", PlaceCategory.RESTAURANT, island, 22,
            "조개구이·해산물 식사 후보", "https://www.diningcode.com/profile.php?rid=FhjkFflBC1jE",
            address = "경기도 시흥시 거북섬로 110"),
        SiheungPlace("food23", "웰컴사이공", PlaceCategory.RESTAURANT, island, 23,
            "베트남 쌀국수·반세오", "https://www.diningcode.com/profile.php?rid=U3kEP1YqJ2ss",
            address = "경기도 시흥시 거북섬2길 25"),
        SiheungPlace("cafe21", "해밀화", PlaceCategory.CAFE, island, 21,
            "바다 전망 베이커리 카페", "https://www.diningcode.com/profile.php?rid=ujMepJpFEE7D",
            address = "경기도 시흥시 거북섬로 116"),
        SiheungPlace("cafe22", "누군가에게 거북섬점", PlaceCategory.CAFE, island, 22,
            "바다 전망·디저트", "https://www.diningcode.com/profile.php?rid=uwEJdsju7pfB",
            address = "경기도 시흥시 거북섬로 110"),
        SiheungPlace("cafe23", "랑데자뷰 시흥거북섬점", PlaceCategory.CAFE, island, 23,
            "커피·바다 전망", "https://www.diningcode.com/profile.php?rid=eSsW9W68keze",
            address = "경기도 시흥시 거북섬4길 10"),
        SiheungPlace("sight01", "거북섬 마리나 경관브릿지", PlaceCategory.ATTRACTION, island, 1,
            "수변 산책·어린왕자 포토존", "https://www.siheung.go.kr/cultour/contents.do?mId=0401170000", 40,
            address = "경기도 시흥시 거북섬로 104", visitNote = "야외 관람은 날씨와 현장 통제를 확인해주세요."),
        SiheungPlace("sight02", "시흥 해양생태과학관", PlaceCategory.ATTRACTION, island, 2,
            "서해 해양생태 전시 관람", "https://www.siheung.go.kr/mesm/itc/guide/information?menuId=8", 90,
            address = "경기도 시흥시 거북섬서로 35", hours = VisitHours(LocalTime.of(10,0), LocalTime.of(18,0)),
            closedWeekdays = setOf(DayOfWeek.MONDAY), closedAnnualDays = setOf(MonthDay.of(1,1)), needsVisitCheck = true,
            visitNote = "정기 10~18시·월요일 휴관. 공휴일 월요일은 화요일 휴관, 1/1·설·추석 및 임시휴관은 공식 공지를 확인해주세요."),
        SiheungPlace("sight03", "시흥오이도박물관", PlaceCategory.ATTRACTION, SiheungArea.OIDO, 3,
            "시흥 선사·해양 생활문화 전시", "https://www.siheung.go.kr/cultour/contents.do?mId=0401130000", 90,
            address = "경기도 시흥시 오이도로 332", hours = VisitHours(LocalTime.of(10,0), LocalTime.of(18,0)),
            closedWeekdays = setOf(DayOfWeek.MONDAY), closedAnnualDays = setOf(MonthDay.of(1,1)), needsVisitCheck = true,
            visitNote = "정기 10~18시·월요일 휴관. 연휴·특별 운영과 체험실 예약은 공식 정보를 확인해주세요."),
        SiheungPlace("sight04", "오이도 빨강등대", PlaceCategory.ATTRACTION, SiheungArea.OIDO, 4,
            "해안 산책·노을 풍경", "https://www.siheung.go.kr/cultour/contents.do?mId=0401110000", 40,
            visitNote = "외부 산책 기준 예상시간이에요. 등대 내부 운영은 현장에서 확인해주세요."),
        SiheungPlace("sight05", "시흥갯골생태공원", PlaceCategory.ATTRACTION, SiheungArea.CENTRAL, 5,
            "갯골·염전 주변 산책", "https://www.siheung.go.kr/cultour/contents.do?mId=0401070000", 90,
            address = "경기도 시흥시 동서로 287", visitNote = "체험 프로그램·시설별 운영과 날씨를 확인해주세요."),
        SiheungPlace("sight06", "배곧한울공원", PlaceCategory.ATTRACTION, SiheungArea.BAEGOT, 6,
            "해안 산책·휴식", "https://www.siheung.go.kr/cultour/contents.do?mId=0401100000", 60,
            address = "경기도 시흥시 해송십리로 61", visitNote = "산책 기준이에요. 해수풀은 계절 운영이므로 별도 확인해주세요."),
        SiheungPlace("sight07", "연꽃테마파크·관곡지", PlaceCategory.ATTRACTION, SiheungArea.CENTRAL, 7,
            "연꽃·역사 경관 산책", cityTour, 60,
            address = "경기도 시흥시 관곡지로 139", visitNote = "개화 시기는 계절과 날씨에 따라 달라요."),
        SiheungPlace("sight08", "물왕호수", PlaceCategory.ATTRACTION, SiheungArea.MULWANG, 8,
            "호수 주변 산책·풍경", "https://www.siheung.go.kr/cultour/contents.do?mId=0401040000", 60,
            address = "경기도 시흥시 금화로 404"),
        SiheungPlace("activity01", "웨이브파크 서핑", PlaceCategory.EXPERIENCE, island, 1,
            "인공파도 서핑 체험·레슨", "https://www.wavepark.co.kr/operatinghours", 120,
            address = "경기도 시흥시 거북섬둘레길 42", needsVisitCheck = true, needsReservationTime = true,
            visitNote = "시즌·방문일 운영 및 예약 세션을 먼저 확인해주세요. 120분은 준비·샤워를 포함한 계획값이며 공식 레슨 85분과 달라요."),
        SiheungPlace("activity02", "거북섬 해양레포츠 요트체험", PlaceCategory.EXPERIENCE, island, 2,
            "시화호 해양레포츠 체험 후보", cityTour, 90,
            address = "경기도 시흥시 거북섬로 104", needsVisitCheck = true, needsReservationTime = true,
            visitNote = "기상·출항 여부와 제공 업체의 실제 예약 시간을 확인해주세요. 90분은 계획용 예상값이에요."),
    )
}
