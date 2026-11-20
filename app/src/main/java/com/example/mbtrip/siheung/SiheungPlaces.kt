package com.example.mbtrip.siheung

/*
 * ───────────── 시흥 장소 목록 ─────────────
 * 팀에서 정리한 문서(시흥 명소 50곳 · 맛집 23곳 · 카페 23곳)를 그대로 옮겼다.
 * 좌표는 문서에 없어서, 앱이 처음 지도를 열 때 주소로 찾아서 폰에 저장해 둔다 (PlaceLocator.kt).
 *
 * area = 대략 어느 동네인지 (추천 순서를 짤 때 가까운 곳끼리 묶는 데 쓴다)
 */

enum class PlaceCategory(val label: String, val tag: String) {
    SIGHT("명소", "명소"),
    FOOD("맛집", "맛집"),
    CAFE("카페", "카페"),
}

data class SiheungPlace(
    val id: String,
    val name: String,
    val address: String,
    val category: PlaceCategory,
    /** 동네 묶음 */
    val area: String,
    /** 보통 머무는 시간 (분) */
    val stayMinutes: Int = 60,
    /** 짧은 안내 (휴무일 등) */
    val note: String? = null,
)

object SiheungPlaces {
    private const val OIDO = "오이도"
    private const val GEOBUK = "거북섬"
    private const val BAEGOT = "배곧"
    private const val GAETGOL = "갯골·월곶"
    private const val MULWANG = "물왕·목감"
    private const val GWANGOK = "관곡지·하중"
    private const val SORAE = "소래산·신천·은계"
    private const val JEONGWANG = "정왕"
    private const val ETC = "시흥"

    private fun s(n: Int, name: String, address: String, area: String, stay: Int = 60, note: String? = null) =
        SiheungPlace("sight_$n", name, "경기도 $address", PlaceCategory.SIGHT, area, stay, note)

    private fun f(n: Int, name: String, address: String, area: String, note: String? = null) =
        SiheungPlace("food_$n", name, address, PlaceCategory.FOOD, area, 70, note)

    private fun c(n: Int, name: String, address: String, area: String, note: String? = null) =
        SiheungPlace("cafe_$n", name, address, PlaceCategory.CAFE, area, 60, note)

    /** 명소 50곳 */
    val sights: List<SiheungPlace> = listOf(
        s(1, "해양생태과학관", "시흥시 검북섬서로 35", GEOBUK, 80, "월요일 휴무"),
        s(2, "프리미엄아울렛", "시흥시 서해안로 699", JEONGWANG, 120),
        s(3, "더쿠통신", "시흥시 서울대학로 278번길 61", BAEGOT, 50),
        s(4, "맑은물상상누리", "시흥시 공단 2대로 14", JEONGWANG, 60),
        s(5, "시흥하늘휴게소 갤러리 오라", "시흥시 수도권제1순환고속도로 105", ETC, 40),
        s(6, "배곧한울공원", "시흥시 해송십리로 61", BAEGOT, 70, "노을 명소"),
        s(7, "오이도", "시흥시 정왕동", OIDO, 90),
        s(8, "갯골생태공원", "시흥시 동서로 287", GAETGOL, 110),
        s(9, "관곡지", "시흥시 하중동 208", GWANGOK, 40, "여름 연꽃"),
        s(10, "물왕호수", "시흥시 물왕동", MULWANG, 60),
        s(11, "미생의 다리", "시흥시 방산동 779-21", GAETGOL, 40, "노을 명소"),
        s(12, "배곧도서관", "시흥시 배곧4로 102", BAEGOT, 60),
        s(13, "배곧생명공원", "시흥시 배곧2로 25", BAEGOT, 60),
        s(14, "비둘기 공원", "시흥시 은행동 551", SORAE, 40),
        s(15, "생명의나무전망대", "시흥시 오이도로 170", OIDO, 30),
        s(16, "소래산", "시흥시 소래산길 37", SORAE, 120, "등산 1~2시간"),
        s(17, "솔내아트센터", "시흥시 호현로 42-8", SORAE, 60),
        s(18, "시흥시탄소중립체험관", "시흥시 경기과기대로 284", JEONGWANG, 60),
        s(19, "시흥오이도박물관", "시흥시 오이도로 332", OIDO, 60),
        s(20, "신천근린공원", "시흥시 포도원로 109번길 9", SORAE, 40),
        s(21, "연꽃테마파크", "시흥시 관곡지로 139", GWANGOK, 60),
        s(22, "오아시스", "시흥시 정왕동 2064", JEONGWANG, 50),
        s(23, "오이도빨강등대", "시흥시 오이도로 170", OIDO, 30),
        s(24, "옥구공원", "시흥시 서해안로 315", JEONGWANG, 70),
        s(25, "월곶포구", "시흥시 월곶동", GAETGOL, 50),
        s(26, "은계호수공원", "시흥시 은행동 601-150", SORAE, 50),
        s(27, "인다바", "시흥시 정왕동 876-363", JEONGWANG, 60),
        s(28, "갯골캠핑장", "시흥시 월곶동 520-240", GAETGOL, 120),
        s(29, "빅트리", "시흥시 동서로 811번길 46-1", MULWANG, 60),
        s(30, "시흥 오이도 선사유적공원", "시흥시 서해안로 113-27", OIDO, 50),
        s(31, "용도수목원", "시흥시 매화동 수인로 32", ETC, 70),
        s(32, "소전미술관", "시흥시 소래산길 41", SORAE, 50),
        s(33, "청화공간", "시흥시 호현로 155-25", SORAE, 60),
        s(34, "호조벌", "시흥시 하상동 8-2", GWANGOK, 50),
        s(35, "중앙공원", "시흥시 정왕2동 1845-1", JEONGWANG, 40),
        s(36, "소망공원", "시흥시 정왕동 1729", JEONGWANG, 40),
        s(37, "희망공원", "시흥시 정왕동 1728", JEONGWANG, 40),
        s(38, "정왕체육공원", "시흥시 정왕동 2158", JEONGWANG, 50),
        s(39, "산들공원", "시흥시 거모동 1354", ETC, 40),
        s(40, "곰솔누리숲", "시흥시 정왕3동 1946", JEONGWANG, 50),
        s(41, "능곡 선사유적지", "시흥시 능곡동 산17-2", ETC, 40),
        s(42, "시흥 소래산 마애보살입상", "시흥시 대야동 산 140-3", SORAE, 30),
        s(43, "시흥문화원", "시흥시 연성로13번길 3", GWANGOK, 40),
        s(44, "군자봉", "시흥시 군자동 산 3-1", ETC, 90, "등산"),
        s(45, "보통천 산책로", "시흥시 정왕동 일대", JEONGWANG, 40),
        s(46, "시흥시청 앞 생태체육공원", "시흥시 장현동", GWANGOK, 40),
        s(47, "시흥에코센터", "시흥시 경기과기대로 284", JEONGWANG, 50),
        s(48, "시흥시청소년수련관", "시흥시 정왕대로233번길 33", JEONGWANG, 60),
        s(49, "하중동고인돌", "시흥시 하중동 산 84", GWANGOK, 30),
        s(50, "영축산", "시흥시 정왕동 산 87-1", JEONGWANG, 70),
    )

    /** 맛집 23곳 */
    val foods: List<SiheungPlace> = listOf(
        f(1, "본가 만두전골전문점", "경기도 시흥시 동서로811번길 33", MULWANG),
        f(2, "봉순게장 물왕리점", "경기도 시흥시 물왕수변로 22", MULWANG),
        f(3, "청춘조개 오이도본점", "경기도 시흥시 오이도로 199-1", OIDO),
        f(4, "참소예", "경기도 시흥시 동서로 706-27", MULWANG),
        f(5, "진짬뽕 진차이나", "경기도 시흥시 동서로 691", MULWANG),
        f(6, "카츠오모이 배곧", "경기도 시흥시 서울대학로278번길 21, 새롬프라자2 1층 108호", BAEGOT),
        f(7, "조개포차 오이도본점", "경기도 시흥시 오이도로 215", OIDO),
        f(8, "소래버섯나라", "경기도 시흥시 동서로 206", GAETGOL),
        f(9, "814바베큐", "경기도 시흥시 서촌상가3길 5, 1층", JEONGWANG),
        f(10, "함돈 본점", "경기도 시흥시 중심상가3길 31-1, 101~102호", JEONGWANG),
        f(11, "이루다제면소 본점", "경기도 시흥시 동서로 768, 1층", MULWANG),
        f(12, "오이도조개92 본점", "경기도 시흥시 오이도로 135", OIDO),
        f(13, "정아각 시화점", "경기도 시흥시 군자천로 335, 1층 107호", JEONGWANG),
        f(14, "산현농장", "경기도 시흥시 동서로 902-96", MULWANG),
        f(15, "서운칼국수", "경기도 시흥시 시흥대로 248-8", SORAE),
        f(16, "고구려짬뽕집 본점", "경기도 시흥시 수인로 3472-22", ETC),
        f(17, "장금이", "경기도 시흥시 피울길 167", ETC),
        f(18, "앞뜰 장작구이", "경기도 시흥시 호현로 294", SORAE),
        f(19, "조개88 오이도본점", "경기도 시흥시 오이도로 131", OIDO),
        f(20, "정씨함박 배곧점", "경기도 시흥시 서울대학로278번길 26, 2층", BAEGOT),
        f(21, "연평도꽃게탕보리밥 오이도거북섬점", "경기도 시흥시 거북섬로 104, 마리나베이101 2블럭 A동 2층", GEOBUK),
        f(22, "노을조개 거북섬", "경기도 시흥시 거북섬1로 4, S프라자 1층 110~111호", GEOBUK),
        f(23, "웰컴사이공", "경기도 시흥시 거북섬2길 25, 퍼스트에비뉴2차 5층 508호", GEOBUK),
    )

    /** 카페 23곳 */
    val cafes: List<SiheungPlace> = listOf(
        c(1, "청화공간", "경기도 시흥시 호현로 155-25", SORAE),
        c(2, "늘솜당", "경기도 시흥시 하우로122번길 50-3", GWANGOK),
        c(3, "베니어 베이커리카페", "경기도 시흥시 서해안로 1444-1", GAETGOL),
        c(4, "빅트리", "경기도 시흥시 동서로811번길 46-1, 2층", MULWANG),
        c(5, "레이크우드", "경기도 시흥시 동서로 810, 1~3층", MULWANG),
        c(6, "블루트렁크 시흥 거북섬점", "경기도 시흥시 거북섬로 110, 마리나베이101 B동 3층 350~353호", GEOBUK),
        c(7, "로아커피", "경기도 시흥시 오이도로 207, 1~3층", OIDO),
        c(8, "기브닝커피", "경기도 시흥시 서해안로1780번길 9", GAETGOL),
        c(9, "커피와일드", "경기도 시흥시 물왕수변로 64", MULWANG),
        c(10, "빛담 & 여정", "경기도 시흥시 동서로811번길 13", MULWANG),
        c(11, "플라워팟 베이커리 카페", "경기도 시흥시 동서로857번길 28-18", MULWANG),
        c(12, "파인커피로스터스", "경기도 시흥시 은행로155번길 17, 은행프라자 1층 106~107호", SORAE),
        c(13, "워터킹커피로스터스", "경기도 시흥시 금화로332번길 13-26", ETC),
        c(14, "카페 연", "경기도 시흥시 관곡지로 140", GWANGOK),
        c(15, "코야드", "경기도 시흥시 배곧전원로 40-47, 1층 101호", BAEGOT),
        c(16, "캄", "경기도 시흥시 서울대학로278번길 8, 아브뉴프랑 센트럴레드 A동 3층 349호", BAEGOT),
        c(17, "클래시커피로스터스", "경기도 시흥시 서울대학로278번길 61", BAEGOT),
        c(18, "할슈타트", "경기도 시흥시 목감전원로 34, 105호", MULWANG),
        c(19, "카페 제니래빗", "경기도 시흥시 섬말길 44", ETC),
        c(20, "카페더피치", "경기도 시흥시 정왕대로233번길 19-1", JEONGWANG),
        c(21, "해밀화", "경기도 시흥시 거북섬로 116, 2층", GEOBUK),
        c(22, "누군가에게 거북섬점", "경기도 시흥시 거북섬로 110, 마리나베이101 B동 2층 246호", GEOBUK),
        c(23, "랑데자뷰 시흥거북섬점", "경기도 시흥시 거북섬4길 10, 시온프라자 2층", GEOBUK),
    )

    val all: List<SiheungPlace> = sights + foods + cafes

    /** 지도에 찍을 목록 — 명소와 카페에 같은 이름(빅트리·청화공간)이 있으면 하나만 */
    val forMap: List<SiheungPlace> = all.distinctBy { it.name }

    private val byName: Map<String, SiheungPlace> = all.asReversed().associateBy { it.name } // 같은 이름이면 앞쪽(명소)
    private val byId: Map<String, SiheungPlace> = all.associateBy { it.id }

    fun byId(id: String): SiheungPlace? = byId[id]

    /** 이름으로 찾기 (같은 이름이 명소·카페에 모두 있으면 먼저 나온 쪽) */
    fun named(name: String): SiheungPlace? = byName[name.trim()]

    fun of(category: PlaceCategory): List<SiheungPlace> = when (category) {
        PlaceCategory.SIGHT -> sights
        PlaceCategory.FOOD -> foods
        PlaceCategory.CAFE -> cafes
    }

    /**
     * 주소를 좌표로 찾을 때 쓸 검색어.
     * 상세 호수(", 1층 108호")는 떼고 도로명까지만.
     */
    fun searchAddress(p: SiheungPlace): String = p.address.substringBefore(",").trim()
}
