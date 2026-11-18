package com.example.mbtrip.recommendation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PlaceLinks {
    private fun query(place: SiheungPlace) = URLEncoder.encode(place.mapQuery, StandardCharsets.UTF_8.name()).replace("+", "%20")
    fun googleMaps(place: SiheungPlace) = "https://www.google.com/maps/search/?api=1&query=${query(place)}"
    fun naverMap(place: SiheungPlace) = "https://map.naver.com/p/search/${query(place)}"
    fun naverBlog(place: SiheungPlace) = "https://m.search.naver.com/search.naver?ssc=tab.m_blog.all&query=${query(place)}"
}
