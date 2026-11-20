package com.example.mbtrip.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.R
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted

/*
 * ───────────── 지역 고르기 ─────────────
 * 방 만들기의 출발·도착을 글자로 치지 않고 목록에서 고른다.
 * 왼쪽 = 시·도, 오른쪽 = 그 안의 시·군 (위의 검색칸으로 바로 찾을 수도 있음).
 * 고른 값은 "시흥" 처럼 시·군 이름 하나로 돌려준다 (서울·부산 같은 특별·광역시는 그 이름).
 */

/** 시·도 → 시·군 */
object KoreaRegions {
    val popular = listOf("서울", "부산", "제주", "강릉", "경주", "여수", "시흥", "속초")

    val groups: List<Pair<String, List<String>>> = listOf(
        "서울" to listOf("서울"),
        "부산" to listOf("부산"),
        "대구" to listOf("대구"),
        "인천" to listOf("인천", "강화", "옹진"),
        "광주" to listOf("광주"),
        "대전" to listOf("대전"),
        "울산" to listOf("울산"),
        "세종" to listOf("세종"),
        "경기" to listOf(
            "수원", "성남", "고양", "용인", "부천", "안산", "안양", "시흥", "화성", "평택", "의정부", "파주",
            "김포", "광명", "광주(경기)", "군포", "하남", "오산", "이천", "안성", "의왕", "양주", "구리", "포천",
            "남양주", "여주", "동두천", "과천", "가평", "양평", "연천",
        ),
        "강원" to listOf("춘천", "원주", "강릉", "속초", "동해", "삼척", "태백", "평창", "정선", "양양", "홍천", "횡성", "인제", "고성(강원)"),
        "충북" to listOf("청주", "충주", "제천", "단양", "보은", "옥천", "영동", "괴산"),
        "충남" to listOf("천안", "아산", "공주", "보령", "서산", "당진", "논산", "부여", "태안", "홍성"),
        "전북" to listOf("전주", "군산", "익산", "남원", "정읍", "김제", "무주", "부안", "고창"),
        "전남" to listOf("여수", "순천", "목포", "광양", "나주", "담양", "보성", "완도", "해남", "진도", "구례"),
        "경북" to listOf("포항", "경주", "안동", "구미", "김천", "영주", "문경", "상주", "영덕", "울진", "울릉"),
        "경남" to listOf("창원", "김해", "진주", "통영", "거제", "양산", "사천", "밀양", "남해", "하동", "거창"),
        "제주" to listOf("제주", "서귀포"),
    )

    val all: List<Pair<String, String>> = groups.flatMap { (p, cs) -> cs.map { p to it } }
}

/**
 * 지역 고르기 모달 — 화면 가운데에 뜨는 목록.
 * 위: 검색칸 / 자주 찾는 곳 / 아래: 시·도별 시·군 (한 줄로 쭉 내려 보며 고른다)
 * @param title "출발 지역" / "도착 지역"
 * @param selected 지금 값 (목록에서 진하게)
 */
@Composable
fun RegionPickerSheet(
    title: String,
    selected: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    // 두 번 눌려도 한 번만 돌려준다 (모달은 부르는 쪽이 닫는다)
    var picked by remember { mutableStateOf(false) }
    fun pick(city: String) {
        if (picked) return
        picked = true
        onPick(city)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(max = 600.dp)
                .imePadding()
                .enterPop(0, from = 0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(top = 18.dp, bottom = 8.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).bouncyClick(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(R.drawable.ic_mb_close, size = 18.dp, tint = Muted, contentDescription = "닫기") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = mbFieldColors(),
                placeholder = { Text("지역 이름으로 찾기 (예: 강릉, 시흥)") },
                leadingIcon = { MBIcon(R.drawable.ic_mb_search, size = 18.dp, tint = Muted) },
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Line)

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                val q = query.trim()
                if (q.isNotEmpty()) {
                    // 검색 결과
                    val found = KoreaRegions.all.filter { (p, c) -> c.contains(q) || p.contains(q) }
                    if (found.isEmpty()) {
                        Text("'$q' 지역을 찾지 못했어요.", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(vertical = 20.dp))
                    }
                    found.forEach { (p, c) ->
                        RegionRow(c, sub = if (p == c) null else p, selected = c == selected) { pick(c) }
                    }
                } else {
                    GroupTitle("자주 찾는 곳")
                    RegionChips(KoreaRegions.popular, selected) { pick(it) }
                    KoreaRegions.groups.forEach { (province, cities) ->
                        Spacer(Modifier.height(16.dp))
                        GroupTitle(province)
                        RegionChips(cities, selected) { pick(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
}

/**
 * 지역 칸 — 한 줄에 4칸씩 같은 너비로.
 * (FlowRow 는 이 화면에서만 쓰던 부품이고 꺼짐의 유력한 원인이라, 기본 Row 로만 만든다)
 */
@Composable
private fun RegionChips(cities: List<String>, selected: String?, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cities.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c -> RegionCell(c, selected = c == selected, modifier = Modifier.weight(1f)) { onPick(c) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RegionCell(name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Ink else Color.White)
            .border(Hairline, if (selected) Ink else Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else Ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun RegionRow(name: String, sub: String?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClick(shape, onClick = onClick)
            .background(if (selected) Aqua100 else Color.Transparent, shape)
            .then(if (selected) Modifier.border(Hairline, Line, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = Ink)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Muted)
        }
        if (selected) MBIcon(R.drawable.ic_mb_check, size = 18.dp, tint = Aqua700)
    }
}
