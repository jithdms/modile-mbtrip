package com.example.mbtrip.recommendation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var selected by remember { mutableStateOf(setOf<String>()) }
    var preview by remember { mutableStateOf<List<Block>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val candidates = PlaceRecommender.recommend(category, area, 24 * 60)
    val scores = members.filter { it.hasDiagnosis }.map { it.jpScore }
    val ready = members.isNotEmpty() && scores.size == members.size
    val ratio = if (ready) ItineraryAllocator.ratioFor(scores) else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${date.monthValue}/${date.dayOfMonth} 일정 추천") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("선정 후보 40곳 · ${SiheungPlaces.CURATED_ON}\n체류시간은 예상값이에요. 영업시간과 이동시간은 지도에서 확인해주세요.", style = MaterialTheme.typography.bodySmall)
                if (ratio != null) Text("목표 확정 ${ratio.fixedPercent}% · 자유 ${ratio.freePercent}%")
                else Text("모든 동행자가 J-P 진단을 마치면 일정을 배분할 수 있어요.")
                Row {
                    PlaceCategory.entries.forEach { item -> TextButton(onClick = { category = item }) { Text(item.label) } }
                    Box {
                        TextButton(onClick = { areaMenu = true }) { Text(area?.label ?: "전체 지역") }
                        DropdownMenu(areaMenu, onDismissRequest = { areaMenu = false }) {
                            DropdownMenuItem(text = { Text("전체 지역") }, onClick = { area = null; areaMenu = false })
                            SiheungArea.entries.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { area = item; areaMenu = false }) }
                        }
                    }
                }
                Text("선택 ${selected.size}곳 · 기존 일정은 모두 유지해요.")
                LazyColumn(Modifier.heightIn(max = 230.dp)) {
                    items(candidates, key = { it.id }) { place ->
                        Row {
                            Checkbox(place.id in selected, onCheckedChange = { checked ->
                                selected = if (checked) selected + place.id else selected - place.id
                                preview = null; error = null
                            })
                            Column(Modifier.weight(1f)) {
                                Text(place.name)
                                Text("${place.area.label} · 예상 ${place.durationMinutes}분\n${place.description}", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = {
                                    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.mapQuery)}")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                        .onFailure { error = "지도를 열 수 있는 앱이 없어요." }
                                }) { Text("지도·영업정보 확인") }
                            }
                        }
                    }
                }
                val result = preview
                if (result != null) {
                    Text("추천 일정 ${PlaceRecommender.addedMinutes(blocks, result)}분 추가 예정", style = MaterialTheme.typography.titleSmall)
                    val oldIds = blocks.map { it.id }.toSet()
                    result.filter { it.id !in oldIds && it.title.isNotBlank() }.forEach { Text("${it.start}~${it.end} ${it.title}", style = MaterialTheme.typography.bodySmall) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = ready && selected.isNotEmpty(), onClick = {
                val current = preview
                if (current == null) {
                    val day = blocks.filter { it.date == date }
                    val start = minOf(LocalTime.of(9,0), day.minOfOrNull { it.start } ?: LocalTime.of(9,0))
                    val end = maxOf(LocalTime.of(21,0), day.maxOfOrNull { it.end } ?: LocalTime.of(21,0))
                    runCatching { PlaceRecommender.previewDay(blocks, tripId, date, start, end, scores,
                        SiheungPlaces.all.filter { it.id in selected }) }
                        .onSuccess {
                            if (PlaceRecommender.addedMinutes(blocks, it) == 0) error = "필수 일정과 목표 비율을 유지하면 추가할 시간이 없어요. 후보를 줄이거나 기존 일정을 확인해주세요."
                            else { preview = it; error = null }
                        }.onFailure { error = it.message }
                } else onApply(current)
            }) { Text(if (preview == null) "배분 미리보기" else "일정에 반영") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
