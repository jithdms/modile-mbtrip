package com.example.mbtrip.recommendation

enum class PlaceCategory(val label: String) { RESTAURANT("맛집"), CAFE("카페") }
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
    val durationMinutes: Int = if (category == PlaceCategory.RESTAURANT) 60 else 40,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && rank > 0 && durationMinutes >= 20)
        require(sourceUrl.startsWith("https://"))
    }
    val mapQuery: String get() = "경기도 시흥시 $name"
}

object SiheungPlaces {
    const val CURATED_ON = "2026-11-17"
    private fun food(id: String, name: String, area: SiheungArea, rank: Int, description: String, rid: String) =
        SiheungPlace(id, name, PlaceCategory.RESTAURANT, area, rank, description, "https://www.diningcode.com/profile.php?rid=$rid")
    private fun cafe(id: String, name: String, area: SiheungArea, rank: Int, description: String, rid: String) =
        SiheungPlace(id, name, PlaceCategory.CAFE, area, rank, description, "https://www.diningcode.com/profile.php?rid=$rid")

    val all: List<SiheungPlace> = listOf(
        food("food01", "본가 만두전골전문점", SiheungArea.MULWANG, 1, "수제만두·전골", "GgJSKRSGqbBJ"),
        food("food02", "봉순게장 물왕리점", SiheungArea.MULWANG, 2, "간장게장·양념게장 정식", "2xcnCZrPQN37"),
        food("food03", "청춘조개 오이도본점", SiheungArea.OIDO, 3, "조개구이·해산물", "8xhT5X6mxj8p"),
        food("food04", "참소예", SiheungArea.MULWANG, 4, "주꾸미볶음·도토리전", "gHjo6thXvcVw"),
        food("food05", "진짬뽕 진차이나", SiheungArea.MULWANG, 5, "짬뽕·탕수육", "sBVsmeobUjL8"),
        food("food06", "카츠오모이 배곧", SiheungArea.BAEGOT, 6, "돈카츠", "rB7ng0X31Ttr"),
        SiheungPlace("food07", "조개포차 오이도본점", PlaceCategory.RESTAURANT, SiheungArea.OIDO, 7,
            "조개구이·해산물", "https://restaurantguru.com/%EC%A1%B0%EA%B0%9C%ED%8F%AC%EC%B0%A8-Siheung-si"),
        food("food08", "소래버섯나라", SiheungArea.CENTRAL, 8, "버섯샤브샤브", "VN0v0Pa7WD40"),
        food("food09", "814바베큐", SiheungArea.JEONGWANG, 9, "우대갈비·삼겹살", "FnOG6stmsEZx"),
        SiheungPlace("food10", "함돈 본점", PlaceCategory.RESTAURANT, SiheungArea.JEONGWANG, 10,
            "고기구이", "https://restaurantguru.com/Siheung-si"),
        food("food11", "이루다제면소 본점", SiheungArea.MULWANG, 11, "우동·소바·돈카츠", "XRZPBRZjLrph"),
        food("food12", "오이도조개92 본점", SiheungArea.OIDO, 12, "조개구이·해산물", "Z9b3GPTy5lAC"),
        food("food13", "정아각 시화점", SiheungArea.JEONGWANG, 13, "바지락짬뽕", "EhXzVwSTVH94"),
        food("food14", "산현농장", SiheungArea.MULWANG, 14, "닭·오리백숙", "24RPCUxlStF2"),
        // Broad regional grouping only; exact addresses and opening hours must be checked on the map.
        food("food15", "서운칼국수", SiheungArea.CENTRAL, 15, "칼국수·보리밥", "Eecv1riSkYCM"),
        food("food16", "고구려짬뽕집 본점", SiheungArea.NORTH, 16, "불짬뽕", "W4ALuxVnvkSa"),
        food("food17", "장금이", SiheungArea.MULWANG, 17, "연잎밥·연요리 코스", "HR5GJuKgBVeY"),
        food("food18", "앞뜰 장작구이", SiheungArea.NORTH, 18, "오리장작구이", "z42R51KXutqx"),
        food("food19", "조개88 오이도본점", SiheungArea.OIDO, 19, "조개구이·찜", "xyEeAHschTos"),
        food("food20", "정씨함박 배곧점", SiheungArea.BAEGOT, 20, "함박스테이크·파스타", "5Y1u4DoEAp9v"),
        cafe("cafe01", "청화공간", SiheungArea.NORTH, 1, "한옥·정원·베이커리", "4lrqDfRmwXP5"),
        cafe("cafe02", "늘솜당", SiheungArea.NORTH, 2, "베이커리·야외 공간", "mip0CHnD3NAk"),
        cafe("cafe03", "베니어 베이커리카페", SiheungArea.NORTH, 3, "대형 베이커리", "4DXkcAkHI845"),
        cafe("cafe04", "빅트리", SiheungArea.MULWANG, 4, "야외정원·계절 풍경", "eVqgEi46eq1K"),
        cafe("cafe05", "레이크우드", SiheungArea.MULWANG, 5, "호수 전망·베이커리", "BEhv8VjcGNKG"),
        cafe("cafe06", "블루트렁크 오이도거북섬점", SiheungArea.GEOBUKSEOM, 6, "바다 전망·베이커리", "YfYD9J7E1W4l"),
        cafe("cafe07", "로아커피", SiheungArea.OIDO, 7, "바다 전망·휘낭시에", "Abnl1H7EL0je"),
        cafe("cafe08", "기브닝커피", SiheungArea.NORTH, 8, "브런치·테라스", "9tB1NXonxveh"),
        cafe("cafe09", "커피와일드", SiheungArea.MULWANG, 9, "핸드드립·호수 전망", "Hfo3e21uAgvF"),
        cafe("cafe10", "빛담 & 여정", SiheungArea.MULWANG, 10, "베이커리·넓은 좌석", "FlbzjEuP6q1r"),
        cafe("cafe11", "플라워팟 베이커리 카페", SiheungArea.MULWANG, 11, "베이커리·테라스", "ov53jxWwBXjx"),
        cafe("cafe12", "파인커피로스터스", SiheungArea.NORTH, 12, "로스터리·커피", "HAyMx8EKQLHO"),
        cafe("cafe13", "워터킹커피로스터스", SiheungArea.MULWANG, 13, "로스터리·테라스", "VS3KyNISRieE"),
        cafe("cafe14", "카페 연", SiheungArea.CENTRAL, 14, "관곡지·연꽃테마파크 산책", "NfazyVj7sO5d"),
        cafe("cafe15", "코야드", SiheungArea.BAEGOT, 15, "커피·티라미수", "fWKkDNxroU5x"),
        cafe("cafe16", "캄", SiheungArea.BAEGOT, 16, "노을·바다 전망·케이크", "RMpg75A8LMDl"),
        cafe("cafe17", "클래시커피로스터스", SiheungArea.BAEGOT, 17, "로스터리·커피", "DIQntPa1TGBb"),
        cafe("cafe18", "할슈타트", SiheungArea.MULWANG, 18, "호수 전망·넓은 좌석", "x1bu5BWWc2Uo"),
        cafe("cafe19", "카페 제니래빗", SiheungArea.CENTRAL, 19, "북카페·들판 전망", "isgeOwXiYs4Y"),
        cafe("cafe20", "카페더피치", SiheungArea.JEONGWANG, 20, "크로플·라떼", "HNrwWdhJBYcb"),
    )
}
