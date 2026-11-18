package com.example.mbtrip.recommendation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mbtrip.diagnosis.ItineraryAllocator
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.Member
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun RecommendationDialog(
    blocks: List<Block>, members: List<Member>, tripId: String, date: LocalDate,
    onDismiss: () -> Unit, onApply: (List<Block>) -> Unit,
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(PlaceCategory.RESTAURANT) }
    var area by remember { mutableStateOf<SiheungArea?>(null) }
    var areaMenu by remember { mutableStateOf(false) }
    var durationMenu by remember { mutableStateOf(false) }
    var maxMinutes by remember { mutableIntStateOf(240) }
    var selected by remember(tripId, date) { mutableStateOf(setOf<String>()) }
    var checked by remember(tripId, date) { mutableStateOf(setOf<String>()) }
    var reservationTexts by remember(tripId, date) { mutableStateOf(mapOf<String, String>()) }
    // A fresh server block/member update invalidates the old plan before it can overwrite it.
    var preview by remember(blocks.toList(), members.toList(), tripId, date, selected, checked, reservationTexts) { mutableStateOf<List<Block>?>(null) }
    var error by remember(blocks, members, tripId, date) { mutableStateOf<String?>(null) }
    val existingNames = blocks.filter { it.date == date }.mapNotNull { it.place }.toSet()
    val candidates = PlaceRecommender.recommend(category, area, maxMinutes, existingNames, date = date)
    val scores = members.filter { it.hasDiagnosis }.map { it.jpScore }
    val ready = members.isNotEmpty() && scores.size == members.size
    val ratio = if (ready) ItineraryAllocator.ratioFor(scores) else null
    val open: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { error = "정보를 열 수 있는 앱이 없어요." }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${date.monthValue}/${date.dayOfMonth} 일정 추천") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("선정 후보 ${SiheungPlaces.all.size}곳 · ${SiheungPlaces.CURATED_ON}\n체류시간은 예상값이며 이동시간은 별도 확인해주세요.", style = MaterialTheme.typography.bodySmall)
            if (ratio != null) Text("목표 확정 ${ratio.fixedPercent}% · 자유 ${ratio.freePercent}%")
            else Text("모든 동행자가 J-P 진단을 마치면 일정을 배분할 수 있어요.")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                PlaceCategory.entries.forEach { item ->
                    TextButton(onClick = { category = item }) { Text(if (category == item) "✓ ${item.label}" else item.label) }
                }
            }
            Row {
                Box {
                    TextButton(onClick = { areaMenu = true }) { Text(area?.label ?: "전체 지역") }
                    DropdownMenu(areaMenu, onDismissRequest = { areaMenu = false }) {
                        DropdownMenuItem(text = { Text("전체 지역") }, onClick = { area = null; areaMenu = false })
                        SiheungArea.entries.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { area = item; areaMenu = false }) }
                    }
                }
                Box {
                    TextButton(onClick = { durationMenu = true }) { Text("체류 ${maxMinutes}분 이내") }
                    DropdownMenu(durationMenu, onDismissRequest = { durationMenu = false }) {
                        listOf(30, 60, 120, 240, 720).forEach { minutes ->
                            DropdownMenuItem(text = { Text("체류 ${minutes}분 이내") }, onClick = { maxMinutes = minutes; durationMenu = false })
                        }
                    }
                }
            }
            Text("선택 ${selected.size}곳 · 기존 일정은 모두 유지해요.", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.heightIn(max = 230.dp)) {
                if (candidates.isEmpty()) item { Text("조건에 맞는 장소가 없어요. 지역·종류·체류시간을 바꿔주세요.") }
                items(candidates, key = { it.id }) { place ->
                    Row {
                        Checkbox(place.id in selected, enabled = !place.needsVisitCheck || place.id in checked,
                            modifier = Modifier.semantics { contentDescription = "선택: ${place.name}" },
                            onCheckedChange = { value -> selected = if (value) selected + place.id else selected - place.id; error = null })
                        Column(Modifier.weight(1f)) {
                            Text(place.name)
                            Text("${place.area.label} · 예상 ${place.durationMinutes}분\n${place.description}", style = MaterialTheme.typography.bodySmall)
                            place.hours?.let { Text("정기 관람 ${it.opens}~${it.closes}", style = MaterialTheme.typography.bodySmall) }
                            place.visitNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (place.needsVisitCheck) {
                                Row {
                                    Checkbox(place.id in checked,
                                        modifier = Modifier.semantics { contentDescription = "운영확인: ${place.name}" },
                                        onCheckedChange = { value ->
                                            checked = if (value) checked + place.id else checked - place.id
                                            if (!value) selected = selected - place.id
                                        })
                                    Text("방문일 운영·예약 정보를 확인했어요", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (place.needsReservationTime && place.id in checked) OutlinedTextField(
                                value = reservationTexts[place.id].orEmpty(),
                                onValueChange = { reservationTexts = reservationTexts + (place.id to it) },
                                label = { Text("예약 시작 HH:mm") }, singleLine = true,
                                modifier = Modifier.semantics { contentDescription = "예약시각: ${place.name}" })
                            Row {
                                TextButton(onClick = { open(PlaceLinks.googleMaps(place)) }) { Text("구글지도") }
                                TextButton(onClick = { open(PlaceLinks.naverMap(place)) }) { Text("네이버지도") }
                            }
                            Row {
                                TextButton(onClick = { open(PlaceLinks.naverBlog(place)) }) { Text("블로그 후기") }
                                TextButton(onClick = { open(place.sourceUrl) }) { Text("운영·선정 근거") }
                            }
                        }
                    }
                }
            }
            preview?.let { result ->
                val oldIds = blocks.map { it.id }.toSet()
                val added = result.filter { it.id !in oldIds && it.title.isNotBlank() }
                Text("추천 ${added.size}곳 · ${PlaceRecommender.addedMinutes(blocks, result)}분 추가 예정", style = MaterialTheme.typography.titleSmall)
                val omitted = SiheungPlaces.all.filter { it.id in selected && added.none { b -> b.place == it.name } }
                if (omitted.isNotEmpty()) Text("운영시간·기존 일정·목표 비율로 제외: ${omitted.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                added.take(3).forEach { Text("${it.start}~${it.end} ${it.title}", style = MaterialTheme.typography.bodySmall) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = ready && selected.isNotEmpty(), onClick = {
            val result = preview
            if (result != null) onApply(result)
            else runCatching {
                val picked = SiheungPlaces.all.filter { it.id in selected }
                val starts = picked.filter { it.needsReservationTime }.associate { place ->
                    val text = reservationTexts[place.id].orEmpty()
                    require(Regex("\\d{2}:\\d{2}").matches(text)) { "${place.name}의 예약 시작 시각을 HH:mm으로 입력해주세요." }
                    place.id to runCatching { LocalTime.parse(text) }.getOrElse { throw IllegalArgumentException("예약 시작 시각을 확인해주세요.") }
                }
                val day = blocks.filter { it.date == date }
                val start = minOf(LocalTime.of(9,0), day.minOfOrNull { it.start } ?: LocalTime.of(9,0))
                val end = maxOf(LocalTime.of(21,0), day.maxOfOrNull { it.end } ?: LocalTime.of(21,0))
                PlaceRecommender.previewDay(blocks, tripId, date, start, end, scores, picked, starts)
            }.onSuccess {
                if (PlaceRecommender.addedMinutes(blocks, it) == 0) error = "운영시간·기존 일정·목표 비율을 유지하면 추가할 시간이 없어요. 선택을 줄이거나 일정을 확인해주세요."
                else { preview = it; error = null }
            }.onFailure { error = it.message }
        }) { Text(if (preview == null) "배분 미리보기" else "일정에 반영") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}
