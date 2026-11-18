package com.example.mbtrip.recommendation


/** Source-checked on 2026-11-18. Editorial candidates, never Google/Naver star rankings. */
internal object AdditionalSiheungPlaces {
    private val island = SiheungArea.GEOBUKSEOM
    val all = listOf(
        SiheungPlace("food21", "연평도꽃게탕보리밥 오이도거북섬점", PlaceCategory.RESTAURANT, island, 21,
            "게장·보리밥 식사 후보", "https://www.diningcode.com/profile.php?rid=ISFr3eYFWUTx",
            address = "경기도 시흥시 거북섬로 104, 마리나베이101 2블럭 A동 2층"),
        SiheungPlace("food22", "노을조개 거북섬", PlaceCategory.RESTAURANT, island, 22,
            "조개구이·해산물 식사 후보", "https://m.place.naver.com/restaurant/1026301238/home",
            address = "경기도 시흥시 거북섬로 110, 마리나베이101 2층 252호"),
        SiheungPlace("food23", "웰컴사이공", PlaceCategory.RESTAURANT, island, 23,
            "베트남 쌀국수·반세오", "https://www.diningcode.com/profile.php?rid=U3kEP1YqJ2ss",
            address = "경기도 시흥시 거북섬2길 25, 퍼스트에비뉴2차 5층 508호"),
        SiheungPlace("cafe21", "해밀화", PlaceCategory.CAFE, island, 21,
            "바다 전망 베이커리 카페", "https://www.diningcode.com/profile.php?rid=ujMepJpFEE7D",
            address = "경기도 시흥시 거북섬로 116, 2층"),
        SiheungPlace("cafe22", "누군가에게 거북섬점", PlaceCategory.CAFE, island, 22,
            "바다 전망·디저트", "https://www.diningcode.com/profile.php?rid=uwEJdsju7pfB",
            address = "경기도 시흥시 거북섬로 110, 마리나베이101 B동 2층 246~249호"),
        SiheungPlace("cafe23", "랑데자뷰 시흥거북섬점", PlaceCategory.CAFE, island, 23,
            "커피·바다 전망", "https://www.diningcode.com/profile.php?rid=eSsW9W68keze",
            address = "경기도 시흥시 거북섬4길 10, 시온프라자 2층"),
    )
}
