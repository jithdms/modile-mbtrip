package com.example.mbtrip.recommendation


enum class PlaceCategory(val label: String, val defaultMinutes: Int) {
    RESTAURANT("맛집", 60), CAFE("카페", 40)
}
enum class SiheungArea(val label: String) {
    MULWANG("물왕·목감"), OIDO("오이도"), GEOBUKSEOM("거북섬"),
    BAEGOT("배곧"), JEONGWANG("정왕"), NORTH("대야·신천·은행·계수"),
    CENTRAL("장곡·하중·월곶·군자"),
}

/** Curated candidates, not a live rating ranking. Durations are planning estimates. */
data class SiheungPlace(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val area: SiheungArea,
    val rank: Int,
    val description: String,
    val sourceUrl: String,
    val durationMinutes: Int = category.defaultMinutes,
    val address: String? = null,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && rank > 0 && durationMinutes >= 20)
        require(sourceUrl.startsWith("https://"))
    }
    val mapQuery: String get() = address?.let { "$it $name" } ?: "경기도 시흥시 $name"
}

object SiheungPlaces {
    const val CURATED_ON = "2026-11-18"
    private fun food(id: String, name: String, area: SiheungArea, rank: Int, description: String, rid: String, address: String) =
        SiheungPlace(id, name, PlaceCategory.RESTAURANT, area, rank, description, "https://www.diningcode.com/profile.php?rid=$rid", address = address)
    private fun cafe(id: String, name: String, area: SiheungArea, rank: Int, description: String, rid: String, address: String) =
        SiheungPlace(id, name, PlaceCategory.CAFE, area, rank, description, "https://www.diningcode.com/profile.php?rid=$rid", address = address)

    private val initialPlaces: List<SiheungPlace> = listOf(
        food("food01", "본가 만두전골전문점", SiheungArea.MULWANG, 1, "수제만두·전골", "GgJSKRSGqbBJ", "경기도 시흥시 동서로811번길 33"),
        food("food02", "봉순게장 물왕리점", SiheungArea.MULWANG, 2, "간장게장·양념게장 정식", "2xcnCZrPQN37", "경기도 시흥시 물왕수변로 22"),
        food("food03", "청춘조개 오이도본점", SiheungArea.OIDO, 3, "조개구이·해산물", "8xhT5X6mxj8p", "경기도 시흥시 오이도로 199-1"),
        food("food04", "참소예", SiheungArea.MULWANG, 4, "주꾸미볶음·도토리전", "gHjo6thXvcVw", "경기도 시흥시 동서로 706-27"),
        food("food05", "진짬뽕 진차이나", SiheungArea.MULWANG, 5, "짬뽕·탕수육", "sBVsmeobUjL8", "경기도 시흥시 동서로 691"),
        food("food06", "카츠오모이 배곧", SiheungArea.BAEGOT, 6, "돈카츠", "rB7ng0X31Ttr", "경기도 시흥시 서울대학로278번길 21, 새롬프라자2 1층 108호"),
        SiheungPlace("food07", "조개포차 오이도본점", PlaceCategory.RESTAURANT, SiheungArea.OIDO, 7,
            "조개구이·해산물", "https://www.diningcode.com/profile.php?rid=Q5fOPk4o5Jrt", address = "경기도 시흥시 오이도로 215"),
        food("food08", "소래버섯나라", SiheungArea.CENTRAL, 8, "버섯샤브샤브", "VN0v0Pa7WD40", "경기도 시흥시 동서로 206"),
        food("food09", "814바베큐", SiheungArea.JEONGWANG, 9, "우대갈비·삼겹살", "FnOG6stmsEZx", "경기도 시흥시 서촌상가3길 5, 1층"),
        SiheungPlace("food10", "함돈 본점", PlaceCategory.RESTAURANT, SiheungArea.JEONGWANG, 10,
            "고기구이", "https://www.diningcode.com/profile.php?rid=z90IGal6BEBN", address = "경기도 시흥시 중심상가3길 31-1, 101~102호"),
        food("food11", "이루다제면소 본점", SiheungArea.MULWANG, 11, "우동·소바·돈카츠", "XRZPBRZjLrph", "경기도 시흥시 동서로 768, 1층"),
        food("food12", "오이도조개92 본점", SiheungArea.OIDO, 12, "조개구이·해산물", "Z9b3GPTy5lAC", "경기도 시흥시 오이도로 135"),
        food("food13", "정아각 시화점", SiheungArea.JEONGWANG, 13, "바지락짬뽕", "EhXzVwSTVH94", "경기도 시흥시 군자천로 335, 1층 107호"),
        food("food14", "산현농장", SiheungArea.MULWANG, 14, "닭·오리백숙", "24RPCUxlStF2", "경기도 시흥시 동서로 902-96"),
        // Addresses checked on 2026-11-18; opening hours are not a live feed.
        food("food15", "서운칼국수", SiheungArea.CENTRAL, 15, "칼국수·보리밥", "Eecv1riSkYCM", "경기도 시흥시 시흥대로 248-8"),
        food("food16", "고구려짬뽕집 본점", SiheungArea.NORTH, 16, "불짬뽕", "W4ALuxVnvkSa", "경기도 시흥시 수인로 3472-22"),
        food("food17", "장금이", SiheungArea.MULWANG, 17, "연잎밥·연요리 코스", "HR5GJuKgBVeY", "경기도 시흥시 피울길 167"),
        food("food18", "앞뜰 장작구이", SiheungArea.NORTH, 18, "오리장작구이", "z42R51KXutqx", "경기도 시흥시 호현로 294"),
        food("food19", "조개88 오이도본점", SiheungArea.OIDO, 19, "조개구이·찜", "xyEeAHschTos", "경기도 시흥시 오이도로 131"),
        food("food20", "정씨함박 배곧점", SiheungArea.BAEGOT, 20, "함박스테이크·파스타", "5Y1u4DoEAp9v", "경기도 시흥시 서울대학로278번길 26, 2층"),
        cafe("cafe01", "청화공간", SiheungArea.NORTH, 1, "한옥·정원·베이커리", "4lrqDfRmwXP5", "경기도 시흥시 호현로 155-25"),
        cafe("cafe02", "늘솜당", SiheungArea.NORTH, 2, "베이커리·야외 공간", "mip0CHnD3NAk", "경기도 시흥시 하우로122번길 50-3"),
        cafe("cafe03", "베니어 베이커리카페", SiheungArea.NORTH, 3, "대형 베이커리", "4DXkcAkHI845", "경기도 시흥시 서해안로 1444-1"),
        cafe("cafe04", "빅트리", SiheungArea.MULWANG, 4, "야외정원·계절 풍경", "eVqgEi46eq1K", "경기도 시흥시 동서로811번길 46-1, 2층"),
        cafe("cafe05", "레이크우드", SiheungArea.MULWANG, 5, "호수 전망·베이커리", "BEhv8VjcGNKG", "경기도 시흥시 동서로 810, 1~3층"),
        cafe("cafe06", "블루트렁크 오이도거북섬점", SiheungArea.GEOBUKSEOM, 6, "바다 전망·베이커리", "YfYD9J7E1W4l", "경기도 시흥시 거북섬로 110, 마리나베이101 B동 3층 350~356호"),
        cafe("cafe07", "로아커피", SiheungArea.OIDO, 7, "바다 전망·휘낭시에", "Abnl1H7EL0je", "경기도 시흥시 오이도로 207, 1~3층"),
        cafe("cafe08", "기브닝커피", SiheungArea.NORTH, 8, "브런치·테라스", "9tB1NXonxveh", "경기도 시흥시 서해안로1780번길 9"),
        cafe("cafe09", "커피와일드", SiheungArea.MULWANG, 9, "핸드드립·호수 전망", "Hfo3e21uAgvF", "경기도 시흥시 물왕수변로 64, 1층"),
        cafe("cafe10", "빛담 & 여정", SiheungArea.MULWANG, 10, "베이커리·넓은 좌석", "FlbzjEuP6q1r", "경기도 시흥시 동서로811번길 13"),
        cafe("cafe11", "플라워팟 베이커리 카페", SiheungArea.MULWANG, 11, "베이커리·테라스", "ov53jxWwBXjx", "경기도 시흥시 동서로857번길 28-18"),
        cafe("cafe12", "파인커피로스터스", SiheungArea.NORTH, 12, "로스터리·커피", "HAyMx8EKQLHO", "경기도 시흥시 은행로155번길 17, 은행프라자 1층 106~107호"),
        cafe("cafe13", "워터킹커피로스터스", SiheungArea.MULWANG, 13, "로스터리·테라스", "VS3KyNISRieE", "경기도 시흥시 금화로332번길 13-26"),
        cafe("cafe14", "카페 연", SiheungArea.CENTRAL, 14, "관곡지·연꽃테마파크 산책", "NfazyVj7sO5d", "경기도 시흥시 관곡지로 140"),
        cafe("cafe15", "코야드", SiheungArea.BAEGOT, 15, "커피·티라미수", "fWKkDNxroU5x", "경기도 시흥시 배곧전원로 40-47, 1층 101호"),
        cafe("cafe16", "캄", SiheungArea.BAEGOT, 16, "노을·바다 전망·케이크", "RMpg75A8LMDl", "경기도 시흥시 서울대학로278번길 8, 아브뉴프랑 센트럴레드 A동 3층 349호"),
        cafe("cafe17", "클래시커피로스터스", SiheungArea.BAEGOT, 17, "로스터리·커피", "DIQntPa1TGBb", "경기도 시흥시 서울대학로278번길 61, 서영베니스스퀘어 1층 C-6호"),
        cafe("cafe18", "할슈타트", SiheungArea.MULWANG, 18, "호수 전망·넓은 좌석", "x1bu5BWWc2Uo", "경기도 시흥시 목감전원로 34, 105호"),
        cafe("cafe19", "카페 제니래빗", SiheungArea.CENTRAL, 19, "북카페·들판 전망", "isgeOwXiYs4Y", "경기도 시흥시 섬말길 44"),
        cafe("cafe20", "카페더피치", SiheungArea.JEONGWANG, 20, "크로플·라떼", "HNrwWdhJBYcb", "경기도 시흥시 정왕대로233번길 19-1"),
    )
    val all: List<SiheungPlace> = initialPlaces + AdditionalSiheungPlaces.all
}
