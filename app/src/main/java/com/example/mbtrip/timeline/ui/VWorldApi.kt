package com.example.mbtrip.timeline.ui

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * 브이월드 오픈API — 가게·장소 이름 검색, 좌표 → 상세 주소.
 *  ⚠ 브이월드 인증키 관리에서 활용 API 에 '검색 API' 와 '지오코더 API' 도 체크해야 한다
 *    (지도(WMTS)만 체크하면 지도는 나오는데 검색이 안 됨)
 */

/** 검색 결과 한 줄 */
internal data class PlaceResult(
    val name: String,       // 가게·장소 이름 (주소 검색이면 주소)
    val address: String,    // 상세 주소 (도로명 우선)
    val category: String?,  // 예) 음식점 > 카페
    val lat: Double,
    val lng: Double,
)

/**
 * 가게명·장소명 검색 (시흥 안만). 순서: 장소(POI) → 도로명 주소 → 지번 주소.
 * 실패하면 빈 목록.
 */
internal suspend fun vworldSearch(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
    val key = vworldKey
    if (key.isBlank() || query.isBlank()) return@withContext emptyList()
    val bbox = "${SiheungArea.WEST},${SiheungArea.SOUTH},${SiheungArea.EAST},${SiheungArea.NORTH}"
    val tries = listOf(
        "type=place",
        "type=address&category=road",
        "type=address&category=parcel",
    )
    for (t in tries) {
        val url = "https://api.vworld.kr/req/search?service=search&request=search&version=2.0" +
            "&crs=EPSG:4326&size=10&page=1&format=json&errorformat=json" +
            "&bbox=$bbox&$t&query=${enc(query)}&key=${enc(key)}&domain=${enc(VWORLD_SERVICE_URL)}"
        val items = getJson(url)
            ?.optJSONObject("response")
            ?.optJSONObject("result")
            ?.optJSONArray("items")
            ?: continue
        val list = (0 until items.length()).mapNotNull { i ->
            val obj = items.optJSONObject(i) ?: return@mapNotNull null
            val point = obj.optJSONObject("point") ?: return@mapNotNull null
            val lng = point.optString("x").toDoubleOrNull() ?: return@mapNotNull null
            val lat = point.optString("y").toDoubleOrNull() ?: return@mapNotNull null
            if (!SiheungArea.contains(lat, lng)) return@mapNotNull null
            val addr = obj.optJSONObject("address")
            val road = addr?.optString("road").orEmpty()
            val parcel = addr?.optString("parcel").orEmpty()
            val address = road.ifBlank { parcel }
            val title = obj.optString("title").ifBlank { addr?.optString("bldnm").orEmpty() }.ifBlank { address }
            PlaceResult(
                name = title,
                address = address,
                category = obj.optString("category").takeIf { c -> c.isNotBlank() },
                lat = lat,
                lng = lng,
            )
        }
        if (list.isNotEmpty()) return@withContext list.distinctBy { it.name to it.address }.take(6)
    }
    emptyList()
}

/** 좌표 → 상세 주소 (도로명 우선, 없으면 지번). 실패하면 null */
internal suspend fun vworldAddress(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    val key = vworldKey
    if (key.isBlank()) return@withContext null
    val url = "https://api.vworld.kr/req/address?service=address&request=getAddress&version=2.0" +
        "&crs=epsg:4326&point=$lng,$lat&type=both&zipcode=false&simple=false" +
        "&format=json&errorformat=json&key=${enc(key)}&domain=${enc(VWORLD_SERVICE_URL)}"
    val result = getJson(url)?.optJSONObject("response")?.optJSONArray("result") ?: return@withContext null
    val all = (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    val pick = all.firstOrNull { it.optString("type") == "road" } ?: all.firstOrNull()
    pick?.optString("text")?.takeIf { it.isNotBlank() }
}

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

private fun getJson(url: String): JSONObject? = try {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 7000
        readTimeout = 7000
        setRequestProperty("Referer", VWORLD_SERVICE_URL)
    }
    try {
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val status = json.optJSONObject("response")?.optString("status")
        if (status != "OK") Log.d("MBTripMap", "브이월드 응답 $status: ${body.take(300)}")
        json
    } finally {
        conn.disconnect()
    }
} catch (e: Exception) {
    Log.w("MBTripMap", "브이월드 요청 실패: ${e.message}")
    null
}

/* ───────── 이전 일정과의 거리 ───────── */

/** 두 좌표 사이 직선거리 (m) */
internal fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * asin(sqrt(a))
}

internal fun formatDistance(m: Double): String =
    if (m < 1000) "${(m / 10).roundToInt() * 10}m" else "%.1fkm".format(m / 1000)

/**
 * 대략적인 이동 시간 (분). 길이 구불구불한 걸 감안해 직선거리 × 1.3.
 *  도보 4km/h, 차 25km/h (시내 평균)
 */
internal fun walkMinutes(m: Double): Int = (m * 1.3 / (4000.0 / 60)).roundToInt().coerceAtLeast(1)
internal fun carMinutes(m: Double): Int = (m * 1.3 / (25000.0 / 60)).roundToInt().coerceAtLeast(1)
