package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockDisplay
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Member
import java.time.LocalDate
import java.time.LocalTime

/* ───────────────────────── 색 규칙 (디자인 시스템 확정 전 임시값) ───────────────────────── */

private object TimelineColors {
    val MoveBg = Color(0xFF111111)       // 검정: 기본 이동 시간
    val FixedBg = Color(0xFFAED6F1)      // 하늘색: 확정 블록
    val FreeBg = Color(0xFFA9E5A0)       // 초록: 자유 블록
    val UndecidedBg = Color(0xFFF2F2F2)  // 회색: 미정 블록
    val Border = Color(0xFF9E9E9E)
    val Axis = Color(0xFFBDBDBD)
    val NowLine = Color(0xFFD32F2F)      // 빨간선: 현재 시각
    val SubText = Color(0xFF757575)
}

private data class CardStyle(val bg: Color, val fg: Color, val label: String)

private fun Block.cardStyle(): CardStyle = when (display) {
    BlockDisplay.MOVE -> CardStyle(TimelineColors.MoveBg, Color.White, "이동")
    BlockDisplay.FIXED -> CardStyle(TimelineColors.FixedBg, Color.Black, "확정")
    BlockDisplay.FREE -> CardStyle(TimelineColors.FreeBg, Color.Black, "자유")
    BlockDisplay.UNDECIDED -> CardStyle(
        TimelineColors.UndecidedBg, Color.Black,
        if (status == BlockStatus.VOTING) "투표중" else "미정",
    )
}


/* ───────────────────────── 화면 진입점 ───────────────────────── */

/**
 * P6 일정표 — 1주차 가짜 데이터 버전.
 *
 * @param now 현재 시각 표시선 위치. null 이면 표시하지 않음 (여행 당일에만 넘김).
 * @param onInviteClick 상단 `+` → 이수진(P3) 초대 모달 연결 지점
 * @param onVoteClick   블록 상세의 '투표하기' → 최재성(P7) 투표 모달 연결 지점
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    initialBlocks: List<Block> = FakeBlocks.blocks,
    members: List<Member> = FakeBlocks.members,
    now: LocalTime? = null,
    onInviteClick: () -> Unit = {},
    onVoteClick: (Block) -> Unit = {},
) {
    // 2주차에 ViewModel + StateFlow(Firestore 리스너)로 옮길 상태
    var blocks by remember { mutableStateOf(initialBlocks) }
    val dates = remember(blocks) { blocks.map { it.date }.distinct().sorted() }
    var selectedDate by remember { mutableStateOf(dates.firstOrNull()) }
    var editing by remember { mutableStateOf<Block?>(null) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        MemberBar(members, onInviteClick)
        HorizontalDivider()
        DaySelector(dates, selectedDate) { selectedDate = it }

        val dayBlocks = blocks.filter { it.date == selectedDate }.sortedBy { it.start }
        DayTimeline(dayBlocks, now, onBlockClick = { editing = it })
    }

    editing?.let { target ->
        BlockDetailSheet(
            block = target,
            onDismiss = { editing = null },
            onSave = { updated ->
                blocks = blocks.map { if (it.id == updated.id) updated else it }
                editing = null
            },
            onVoteClick = { onVoteClick(target) },
        )
    }
}

/* ───────────────────────── 상단: 동행자 바 ───────────────────────── */

@Composable
private fun MemberBar(members: List<Member>, onInviteClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Circle(onClick = onInviteClick) { Text("+", fontSize = 24.sp, color = TimelineColors.SubText) }
        members.forEach { m -> Circle { Text(m.nickname.take(1), fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun Circle(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .border(1.dp, TimelineColors.Border, CircleShape)
            .background(Color(0xFFF7F7F7))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/* ───────────────────────── Day 선택 ───────────────────────── */

@Composable
private fun DaySelector(dates: List<LocalDate>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        dates.forEachIndexed { i, d ->
            val isSel = d == selected
            Text(
                "Day${i + 1}",
                fontSize = if (isSel) 28.sp else 18.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) Color.Black else TimelineColors.SubText,
                modifier = Modifier.clickable { onSelect(d) },
            )
        }
    }
}

/* ───────────────────────── 타임라인 본문 ───────────────────────── */

@Composable
private fun DayTimeline(blocks: List<Block>, now: LocalTime?, onBlockClick: (Block) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 16.dp, bottom = 32.dp)) {
        items(buildItems(blocks, now), key = {
            when (it) { is TimelineItem.BlockItem -> it.block.id; TimelineItem.NowItem -> "now" }
        }) { item ->
            when (item) {
                is TimelineItem.BlockItem -> TimelineRow(item.block, onBlockClick)
                TimelineItem.NowItem -> NowIndicator(now!!)
            }
        }
    }
}

@Composable
private fun TimelineRow(block: Block, onClick: (Block) -> Unit) {
    Row(Modifier.fillMaxWidth().height(96.dp)) {
        // 왼쪽 축: 세로선 + 동그라미
        Box(Modifier.width(28.dp).fillMaxHeight()) {
            Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.Center).background(TimelineColors.Axis))
            Box(
                Modifier.padding(top = 10.dp).size(10.dp).align(Alignment.TopCenter)
                    .clip(CircleShape).background(Color.White)
                    .border(1.dp, TimelineColors.Border, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        BlockCard(block, Modifier.weight(1f).padding(vertical = 4.dp)) { onClick(block) }
    }
}

@Composable
private fun BlockCard(block: Block, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val style = block.cardStyle()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxHeight().clip(shape).background(style.bg)
            .border(1.dp, if (block.display == BlockDisplay.MOVE) style.bg else TimelineColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(block.timeText(), fontSize = 11.sp, color = style.fg.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Text(
                style.label + if (block.locked) " · 잠금" else "",
                fontSize = 10.sp, color = style.fg.copy(alpha = 0.7f),
            )
        }
        Text(
            block.title, color = style.fg, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            block.tags.forEach { TagChip(it, style.fg) }
        }
    }
}

@Composable
private fun TagChip(tag: String, fg: Color = Color.Black, selected: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Text(
        "#$tag", fontSize = 10.sp, color = fg,
        modifier = Modifier.clip(shape)
            .background(if (selected) TimelineColors.FreeBg else Color.Transparent)
            .border(1.dp, fg.copy(alpha = 0.6f), shape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun NowIndicator(now: LocalTime) {
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp).height(1.dp).background(TimelineColors.NowLine))
        Box(Modifier.weight(1f).height(1.dp).background(TimelineColors.NowLine))
        Box(Modifier.size(8.dp).background(TimelineColors.NowLine))
        Spacer(Modifier.width(4.dp))
        Text(now.format(HM), fontSize = 10.sp, color = TimelineColors.NowLine)
    }
}

/* ───────────────────────── 블록 상세 시트 ───────────────────────── */

private val TAG_OPTIONS = listOf("맛집", "명소", "카페")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockDetailSheet(
    block: Block,
    onDismiss: () -> Unit,
    onSave: (Block) -> Unit,
    onVoteClick: () -> Unit,
) {
    var tags by remember(block.id) { mutableStateOf(block.tags) }
    var place by remember(block.id) { mutableStateOf(block.place.orEmpty()) }
    var time by remember(block.id) { mutableStateOf(block.timeText()) }
    var memo by remember(block.id) { mutableStateOf(block.memo.orEmpty()) }
    val editable = !block.locked

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TAG_OPTIONS.forEach { t ->
                    val on = t in tags
                    Box(Modifier.clickable(enabled = editable) { tags = if (on) tags - t else tags + t }) {
                        TagChip(t, selected = on)
                    }
                }
            }
            // 지도 자리 — 지도 SDK는 범위 밖, 1주차는 회색 박스
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center,
            ) { Text("지도 자리 (추후)", color = TimelineColors.SubText, fontSize = 12.sp) }

            LabeledField("장소", place, editable) { place = it }
            LabeledField("시간", time, editable) { time = it }
            LabeledField("메모", memo, editable, minHeight = 72) { memo = it }

            if (block.locked) Text("잠긴 블록은 편집할 수 없어요.", fontSize = 11.sp, color = TimelineColors.SubText)

            if (block.status != BlockStatus.CONFIRMED) {
                Button(
                    onClick = onVoteClick, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TimelineColors.FixedBg, contentColor = Color.Black),
                ) { Text("투표하기") }
            }
            Button(
                onClick = { onSave(block.applyEdit(tags, place, time, memo)) },
                enabled = editable, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
            ) { Text("저장하기") }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, enabled: Boolean, minHeight: Int = 0, onChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp)
        OutlinedTextField(
            value = value, onValueChange = onChange, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

/* ───────────────────────── Preview ───────────────────────── */

@Preview(showBackground = true, widthDp = 360, heightDp = 760, name = "Day1 · 현재 14:10")
@Composable
private fun TimelinePreview() {
    TimelineScreen(now = LocalTime.of(14, 10))
}
