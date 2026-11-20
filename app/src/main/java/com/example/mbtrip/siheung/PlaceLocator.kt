package com.example.mbtrip.siheung

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.example.mbtrip.timeline.ui.searchPlace
import com.example.mbtrip.timeline.ui.vworldSearch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** 위도 · 경도 */
data class LatLng(val lat: Double, val lng: Double)

/**
 * 시흥 장소 좌표 찾기.
 * 문서에는 주소만 있어서 ① 주소 → ② "시흥 + 이름" 순서로 찾고, 찾은 좌표는 폰에 저장해 둔다.
 * 그래서 서버에 묻는 건 처음 한 번뿐이고, 다음부터는 지도를 열자마자 점이 찍힌다.
 */
object PlaceLocator {
    private const val PREFS = "siheung_places_geo"

    /** 장소 id → 좌표. 화면이 바로 바뀌도록 Compose 상태 */
    val located = mutableStateMapOf<String, LatLng>()

    /** 찾아봤는데 없던 곳 (같은 앱 실행 중엔 다시 묻지 않음) */
    private val missing = mutableSetOf<String>()
    private var loaded = false
    private val runLock = Mutex()

    fun of(p: SiheungPlace): LatLng? = located[p.id]

    private fun loadCache(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.all.forEach { (id, v) ->
            val parts = (v as? String)?.split(",") ?: return@forEach
            val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: return@forEach
            val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            located[id] = LatLng(lat, lng)
        }
    }

    private fun save(context: Context, id: String, p: LatLng) {
        located[id] = p
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(id, "${p.lat},${p.lng}").apply()
    }

    /** 한 곳 좌표 (이미 알면 바로) */
    suspend fun locate(context: Context, place: SiheungPlace): LatLng? {
        loadCache(context)
        located[place.id]?.let { return it }
        if (place.id in missing) return null
        val found = runCatching { find(context, place) }.getOrNull()
        if (found != null) save(context, place.id, found) else missing += place.id
        return found
    }

    private suspend fun find(context: Context, place: SiheungPlace): LatLng? {
        // 같은 주소를 쓰는 다른 장소(예: 빨강등대·생명의나무전망대)를 이미 찾았으면 그 좌표
        SiheungPlaces.all.firstOrNull { it.id != place.id && it.address == place.address && located[it.id] != null }
            ?.let { return located[it.id] }

        val address = SiheungPlaces.searchAddress(place)
        // "시흥시 정왕동" 처럼 동 이름뿐인 주소는 이름으로 찾는 게 더 정확하다
        val roughAddress = !Regex("\\d").containsMatchIn(address)
        if (!roughAddress) {
            vworldSearch(address, addressFirst = true).firstOrNull()?.let { return LatLng(it.lat, it.lng) }
        }
        vworldSearch("시흥 ${place.name}").firstOrNull()?.let { return LatLng(it.lat, it.lng) }
        if (roughAddress) {
            vworldSearch(address, addressFirst = true).firstOrNull()?.let { return LatLng(it.lat, it.lng) }
        }
        // 브이월드가 안 되면 안드로이드 기본 검색
        searchPlace(context, address)?.let { return LatLng(it.lat, it.lng) }
        return searchPlace(context, place.name)?.let { LatLng(it.lat, it.lng) }
    }

    /** 아직 모르는 곳을 모두 찾는다 (동시에 4곳씩). 여러 화면이 불러도 한 번만 돈다 */
    suspend fun locateAll(context: Context, places: List<SiheungPlace> = SiheungPlaces.forMap) {
        loadCache(context)
        if (places.all { located[it.id] != null || it.id in missing }) return
        runLock.withLock {
            val gate = Semaphore(4)
            coroutineScope {
                places.filter { located[it.id] == null && it.id !in missing }
                    .map { p -> async { gate.withPermit { locate(context, p) } } }
                    .awaitAll()
            }
        }
    }
}
