package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.siheung.LatLng
import com.example.mbtrip.siheung.PlaceCategory
import com.example.mbtrip.siheung.PlaceLocator
import com.example.mbtrip.siheung.SiheungPlace
import com.example.mbtrip.siheung.SiheungPlaces
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import java.time.LocalTime

/*
 * ───────────── 시흥 장소 추천 ─────────────
 *  - 지도: 문서의 명소·맛집·카페를 색 점으로 (rememberSiheungPins)
 *  - 일정 모달: 시간대·이전 일정 위치를 보고 가까운 곳부터 추천 (SiheungRecommendations)
 */

/** 점 색 — 명소 청록 · 맛집 산호 · 카페 갈색 */
internal val PlaceCategory.pinColor: Int
    get() = when (this) {
        PlaceCategory.SIGHT -> android.graphics.Color.rgb(0x1A, 0x8F, 0xB0)
        PlaceCategory.FOOD -> android.graphics.Color.rgb(0xE8, 0x73, 0x5A)
        PlaceCategory.CAFE -> android.graphics.Color.rgb(0x9A, 0x6B, 0x3F)
    }

internal val PlaceCategory.composeColor: Color get() = Color(pinColor)

/** 지도에 찍을 시흥 장소 점들. 좌표를 모르는 곳은 찾는 대로 하나씩 늘어난다 */
@Composable
internal fun rememberSiheungPins(): List<PlacePin> {
    val context = LocalContext.current
    LaunchedEffect(Unit) { runCatching { PlaceLocator.locateAll(context.applicationContext) } }
    val located = PlaceLocator.located
    val count = located.size
    return remember(count) {
        SiheungPlaces.forMap.mapNotNull { p ->
            val ll = located[p.id] ?: return@mapNotNull null
            PlacePin(p.id, ll.lat, ll.lng, p.name, p.category.pinColor)
        }
    }
}

/** 시간대에 맞는 종류: 점심·저녁 = 맛집, 오후 = 카페, 그 밖 = 명소 */
internal fun suggestedCategory(start: LocalTime, prevPlace: SiheungPlace?): PlaceCategory {
    val h = start.hour
    return when {
        h in 11..13 || h in 17..19 -> if (prevPlace?.category == PlaceCategory.FOOD) PlaceCategory.CAFE else PlaceCategory.FOOD
        h in 14..16 -> if (prevPlace?.category == PlaceCategory.CAFE) PlaceCategory.SIGHT else PlaceCategory.CAFE
        else -> PlaceCategory.SIGHT
    }
}

/**
 * 추천 목록: 이미 일정에 넣은 곳은 빼고,
 * 이전 일정 위치를 알면 가까운 순, 모르면 이전 일정과 같은 동네 먼저.
 */
internal fun recommendPlaces(
    category: PlaceCategory,
    prevLocation: LatLng?,
    prevPlace: SiheungPlace?,
    used: Set<String>,
    limit: Int = 10,
): List<SiheungPlace> {
    val candidates = SiheungPlaces.of(category).filter { it.name !in used }
    val sorted = if (prevLocation != null) {
        candidates.sortedBy { p ->
            PlaceLocator.of(p)?.let { distanceMeters(prevLocation.lat, prevLocation.lng, it.lat, it.lng) } ?: Double.MAX_VALUE
        }
    } else if (prevPlace != null) {
        candidates.sortedBy { if (it.area == prevPlace.area) 0 else 1 }
    } else {
        candidates
    }
    return sorted.take(limit)
}

/**
 * 일정 모달의 '시흥 추천' 줄.
 * @param onChoose 고른 장소 (+ 알고 있는 좌표)
 */
@Composable
internal fun SiheungRecommendations(
    draft: Block,
    prev: Block?,
    allBlocks: List<Block>,
    enabled: Boolean,
    onChoose: (SiheungPlace) -> Unit,
) {
    val prevPlace = remember(prev?.place) { prev?.place?.let { SiheungPlaces.named(it) } }
    var category by remember(draft.id) { mutableStateOf(suggestedCategory(draft.start, prevPlace)) }
    val used = remember(allBlocks, draft.id) {
        allBlocks.filter { it.id != draft.id }.mapNotNull { it.place?.trim() }.toSet()
    }
    val prevLocation = prev?.let { p -> if (p.lat != null && p.lng != null) LatLng(p.lat, p.lng) else null }
        ?: prevPlace?.let { PlaceLocator.of(it) }
    val locatedCount = PlaceLocator.located.size
    val list = remember(category, prevLocation, prevPlace, used, locatedCount) {
        recommendPlaces(category, prevLocation, prevPlace, used)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("시흥 추천", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
            PlaceCategory.entries.forEach { c ->
                val on = c == category
                Text(
                    c.label,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) Color.White else Ink,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .bouncyClick(RoundedCornerShape(50)) { category = c }
                        .background(if (on) c.composeColor else Color.White, RoundedCornerShape(50))
                        .border(Hairline, if (on) c.composeColor else Line, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            when {
                prev?.place != null -> "'${prev.displayTitle}' 다음에 가기 좋은 곳이에요."
                else -> "${draft.start.hour}시쯤 가기 좋은 ${category.label}이에요."
            },
            fontSize = 11.sp, color = Muted,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            list.forEach { p ->
                val chosen = draft.place?.trim() == p.name
                val ll = PlaceLocator.of(p)
                val distance = if (prevLocation != null && ll != null) {
                    formatDistance(distanceMeters(prevLocation.lat, prevLocation.lng, ll.lat, ll.lng))
                } else null
                RecommendCard(p, distance, chosen, enabled) { onChoose(p) }
            }
        }
    }
}

@Composable
private fun RecommendCard(place: SiheungPlace, distance: String?, chosen: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(140.dp)
            .bouncyClick(shape, enabled = enabled, onClick = onClick)
            .background(if (chosen) Aqua100 else Color.White, shape)
            .border(Hairline, if (chosen) Aqua500 else Line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(place.category.composeColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(place.area, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            place.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(distance, place.note).joinToString(" · ").ifBlank { "약 ${place.stayMinutes}분" },
            fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 지도 아래 범례: ● 명소 ● 맛집 ● 카페 */
@Composable
internal fun SiheungPinLegend(modifier: Modifier = Modifier, note: String? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PlaceCategory.entries.forEach { c ->
            Box(Modifier.size(8.dp).background(c.composeColor, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(c.label, fontSize = 11.sp, color = Muted)
            Spacer(Modifier.width(10.dp))
        }
        if (note != null) Text(note, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
