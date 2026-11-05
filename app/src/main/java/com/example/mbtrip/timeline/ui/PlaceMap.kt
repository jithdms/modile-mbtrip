package com.example.mbtrip.timeline.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import kotlin.coroutines.resume

/*
 * 일정 모달의 지도 + 장소 검색.
 *  - 지도: osmdroid + 브이월드 배경지도 (OSM 공식 서버는 403 으로 막혀서 안 씀). 키는 MapKey.kt
 *    카카오/네이버 지도로 바꿀 때는 이 파일의 PlaceMap 만 갈아끼우면 된다.
 *  - 검색: 안드로이드 기본 Geocoder (구글 플레이 서비스가 없는 에뮬레이터에선 결과가 없을 수 있음)
 *  - 범위: 시흥시 경계선(SiheungBoundary.kt)을 파란 테두리로 그리고 바깥은 어둡게.
 *          축소·이동은 자유, 핀·검색은 시흥 안만 받는다.
 */

/** 브이월드 배경지도. 주소 형식이 z/y/x 순서라 직접 만든다 */
private class VWorldTileSource(private val key: String) : OnlineTileSourceBase(
    "VWorldBase", 6, 19, 256, ".png",
    arrayOf("https://api.vworld.kr/req/wmts/1.0.0/"),
    "© 국토교통부 브이월드",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + key + "/Base/" +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + ".png"
}

/** 지도 축소 한계 — 7 이면 한반도 전체가 보일 정도. 시흥 밖도 둘러볼 수 있게 넉넉히 */
private const val MIN_ZOOM = 7.0
private const val MAX_ZOOM = 19.0

/* 시흥 경계선 · 바깥 어둡게 */
private val BORDER_COLOR = android.graphics.Color.rgb(0x1E, 0x88, 0xE5)  // 파랑 (체크인 선과 같은 색)
private val OUTSIDE_DIM = android.graphics.Color.argb(0x55, 0x30, 0x30, 0x30) // 시흥 밖 반투명 회색

private const val DEFAULT_ZOOM = 14.0
private const val PIN_ZOOM = 16.0

/**
 * 지도. 핀 = (lat, lng). 지도를 한 번 누르면 그 위치로 핀이 옮겨지고 [onPick] 이 불린다.
 * 지도 안에서 끌기·두 손가락 확대는 지도가 먹고, 바깥 스크롤은 막는다.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun PlaceMap(
    lat: Double?,
    lng: Double?,
    enabled: Boolean,
    onPick: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier,
    onOutside: () -> Unit = {},
    /** 바로 전 일정의 위치 — 있으면 회색 점 + 점선으로 이어서 보여준다 */
    prevLat: Double? = null,
    prevLng: Double? = null,
) {
    // Android Studio 미리보기에서는 지도 대신 회색 상자
    if (LocalInspectionMode.current) {
        Box(modifier.background(Color(0xFFE6EBE3)), contentAlignment = Alignment.Center) {
            Text("지도", fontSize = 13.sp, color = TimelineColors.SubText)
        }
        return
    }

    // 키가 없으면 OSM '접근 차단' 그림 대신 안내 문구
    val key = vworldKey
    Log.d("MBTripMap", if (key.isBlank()) "VWORLD_KEY 없음 → 지도 안 띄움" else "지도: 브이월드 (키 ${key.take(6)}…)")
    if (key.isBlank()) {
        Box(modifier.background(Color(0xFFE6EBE3)).padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                "지도 키(VWORLD_KEY)가 없어요.\nlocal.properties 또는 MapKey.kt 의 VWORLD_KEY_DIRECT 에 넣어주세요.",
                fontSize = 12.sp, color = TimelineColors.SubText, textAlign = TextAlign.Center,
            )
        }
        return
    }

    val context = LocalContext.current
    val pick by rememberUpdatedState(onPick)
    val canPick by rememberUpdatedState(enabled)
    val outside by rememberUpdatedState(onOutside)

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // OSM 정책: 앱을 알아볼 수 있는 이름을 보내야 함 (com.example… 같은 이름은 403 으로 막힘)
            userAgentValue = "MBTrip/1.0 (Android; student trip planner)"
            additionalHttpRequestProperties["Referer"] = VWORLD_SERVICE_URL
        }
        MapView(context).apply {
            setTileSource(VWorldTileSource(key))
            isTilesScaledToDpi = true // 고해상도 폰에서 글자가 너무 작지 않게
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = MIN_ZOOM
            maxZoomLevel = MAX_ZOOM
            controller.setZoom(if (lat != null) PIN_ZOOM else DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(lat ?: SiheungArea.CENTER_LAT, lng ?: SiheungArea.CENTER_LNG))
            // 1) 시흥 밖 어둡게: 큰 네모(한반도 주변)에 시흥 모양 구멍을 뚫는다
            val border = SIHEUNG_BOUNDARY.map { (la, ln) -> GeoPoint(la, ln) }
            overlays.add(
                Polygon(this).apply {
                    setPoints(
                        listOf(
                            GeoPoint(39.5, 124.0), GeoPoint(39.5, 131.0),
                            GeoPoint(33.0, 131.0), GeoPoint(33.0, 124.0),
                        ),
                    )
                    setHoles(listOf(border))
                    fillPaint.color = OUTSIDE_DIM
                    outlinePaint.strokeWidth = 0f
                    outlinePaint.color = android.graphics.Color.TRANSPARENT
                    setOnClickListener { _, _, _ -> false } // 탭은 아래 MapEventsOverlay 로
                },
            )
            // 2) 시흥 테두리
            overlays.add(
                Polygon(this).apply {
                    setPoints(border)
                    fillPaint.color = android.graphics.Color.TRANSPARENT
                    outlinePaint.color = BORDER_COLOR
                    outlinePaint.strokeWidth = 3f * resources.displayMetrics.density
                    setOnClickListener { _, _, _ -> false }
                },
            )
            // 출처 표시 (지도 이용 조건)
            overlays.add(CopyrightOverlay(context))
            // 3) 탭 받기 (맨 위에 둬서 폴리곤이 탭을 먹지 않게)
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (p != null && canPick) {
                            if (SiheungArea.contains(p.latitude, p.longitude)) pick(p.latitude, p.longitude) else outside()
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }),
            )
            // 모달이 위아래로 스크롤되므로, 지도를 만지는 동안은 부모가 손가락을 가로채지 못하게
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ -> true } // 말풍선 안 띄움
        }
    }
    var firstApply by remember { mutableStateOf(true) }

    // 이전 일정: 회색 동그라미 + 점선
    val density = context.resources.displayMetrics.density
    val prevMarker = remember {
        Marker(mapView).apply {
            icon = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(0x75, 0x75, 0x75))
                setStroke((2 * density).toInt(), android.graphics.Color.WHITE)
                setSize((14 * density).toInt(), (14 * density).toInt())
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> true }
        }
    }
    val prevLine = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.rgb(0x75, 0x75, 0x75)
            outlinePaint.strokeWidth = 2.5f * density
            outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8 * density, 6 * density), 0f)
            setOnClickListener { _, _, _ -> false }
        }
    }

    // 화면이 멈추면 지도도 멈추고, 모달이 닫히면 정리
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // 좌표가 바뀌면 핀 옮기기
    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            val p = GeoPoint(lat, lng)
            marker.position = p
            if (!mapView.overlays.contains(marker)) mapView.overlays.add(marker)
            if (firstApply) {
                mapView.controller.setCenter(p)
            } else {
                mapView.controller.animateTo(p, PIN_ZOOM, 600L)
            }
        } else {
            mapView.overlays.remove(marker)
        }
        firstApply = false
        mapView.invalidate()
    }

    LaunchedEffect(prevLat, prevLng, lat, lng) {
        mapView.overlays.remove(prevMarker)
        mapView.overlays.remove(prevLine)
        if (prevLat != null && prevLng != null) {
            val prev = GeoPoint(prevLat, prevLng)
            prevMarker.position = prev
            // 탭 받는 MapEventsOverlay 보다 아래에 깐다
            val below = mapView.overlays.indexOfFirst { it is MapEventsOverlay }.let { if (it < 0) mapView.overlays.size else it }
            if (lat != null && lng != null) {
                prevLine.setPoints(listOf(prev, GeoPoint(lat, lng)))
                mapView.overlays.add(below, prevLine)
            }
            mapView.overlays.add(below, prevMarker)
        }
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/* ───────── 장소 검색 (Geocoder) ───────── */

internal data class PlaceHit(val lat: Double, val lng: Double)

/**
 * 장소 이름 → 좌표 (시흥 안에서만 찾음). 못 찾으면 null.
 * "카페" 처럼 짧은 이름은 전국에서 걸리므로 앞에 "시흥" 을 붙여서 한 번 더 찾는다.
 */
internal suspend fun searchPlace(context: Context, query: String): PlaceHit? {
    if (query.isBlank() || !Geocoder.isPresent()) return null
    return geocodeInSiheung(context, query)
        ?: if (!query.contains("시흥")) geocodeInSiheung(context, "시흥 $query") else null
}

private suspend fun geocodeInSiheung(context: Context, query: String): PlaceHit? {
    val geocoder = Geocoder(context, Locale.KOREA)
    return suspendCancellableCoroutine { cont ->
        try {
            geocoder.getFromLocationName(
                query, 5,
                SiheungArea.SOUTH, SiheungArea.WEST, // 왼쪽 아래
                SiheungArea.NORTH, SiheungArea.EAST, // 오른쪽 위
                object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    // 범위는 '우선' 조건일 뿐이라, 시흥 밖 결과는 한 번 더 걸러낸다
                    val a = addresses.firstOrNull { SiheungArea.contains(it.latitude, it.longitude) }
                    if (cont.isActive) cont.resume(a?.let { PlaceHit(it.latitude, it.longitude) })
                }

                override fun onError(errorMessage: String?) {
                    if (cont.isActive) cont.resume(null)
                }
            },
            )
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }
}

/** 좌표 → 짧은 주소 (예: "종로구 사직로 161"). 못 찾으면 null */
internal suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.KOREA)
    return suspendCancellableCoroutine { cont ->
        try {
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (cont.isActive) cont.resume(addresses.firstOrNull()?.shortLabel())
                }

                override fun onError(errorMessage: String?) {
                    if (cont.isActive) cont.resume(null)
                }
            })
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }
}

private fun Address.shortLabel(): String? {
    val short = listOfNotNull(subLocality ?: locality, thoroughfare, subThoroughfare)
        .distinct()
        .joinToString(" ")
    return short.ifBlank { getAddressLine(0) }?.takeIf { it.isNotBlank() }
}
