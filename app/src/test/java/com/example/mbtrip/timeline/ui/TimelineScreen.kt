package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mbtrip.timeline.data.BlockRepository
import com.example.mbtrip.timeline.data.TimelineDependencies
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.model.TimeMarker
import com.example.mbtrip.timeline.vote.FakeVoteActions
import com.example.mbtrip.timeline.vote.VoteActions
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private data class EditorState(val block: Block, val isNew: Boolean)

/**
 * P6 일정표 — 저장소(Fake 또는 Firestore)와 연결된 진입점. NavGraph 에서는 TimelineScreen() 만 부르면 된다.
 *
 * @param repository   블록 저장소. 기본값은 TimelineDependencies 에서 고른 것 (Firebase 설정 후 Firestore 로 전환)
 * @param voteActions  투표 저장 로직 (최재성 담당). 기본값은 가짜
 * @param markers      체크인·체크아웃 시각 (파란 선)
 * @param now          현재 시각 표시선 위치 (빨간 선). null 이면 표시하지 않음
 * @param onInviteClick 상단 `+` → 초대 모달 연결 지점
 */
@Composable
fun TimelineScreen(
    tripId: String = FakeBlocks.TRIP_ID,
    repository: BlockRepository = TimelineDependencies.blockRepository,
    voteActions: VoteActions = TimelineDependencies.voteActions,
    members: List<Member> = FakeBlocks.members,
    tripStart: LocalDate = FakeBlocks.TRIP_START,
    tripEnd: LocalDate = FakeBlocks.TRIP_END,
    markers: List<TimeMarker> = FakeBlocks.markers,
    now: LocalTime? = null,
    onInviteClick: () -> Unit = {},
) {
    val vm: TimelineViewModel = viewModel(
        key = "timeline-$tripId",
        factory = TimelineViewModel.factory(repository, tripId),
    )
    val blocks by vm.blocks.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    TimelineContent(
        blocks = blocks,
        tripId = tripId,
        members = members,
        tripStart = tripStart,
        tripEnd = tripEnd,
        markers = markers,
        now = now,
        snackbar = snackbar,
        voteActions = voteActions,
        onAdd = vm::add,
        onUpdate = vm::update,
        onDelete = vm::delete,
        onMove = vm::move,
        onSeed = vm::seedFakeData,
        onInviteClick = onInviteClick,
    )
}

/**
 * 일정표 화면 그리기. 저장은 하지 않고, 규칙 검사 결과(EditResult)를 돌려주는 콜백만 부른다.
 * @param blocks null = 불러오는 중
 */
@Composable
internal fun TimelineContent(
    blocks: List<Block>?,
    tripId: String,
    members: List<Member>,
    tripStart: LocalDate,
    tripEnd: LocalDate,
    markers: List<TimeMarker>,
    now: LocalTime?,
    snackbar: SnackbarHostState,
    voteActions: VoteActions,
    onAdd: (Block) -> EditResult,
    onUpdate: (original: Block, edited: Block) -> EditResult,
    onDelete: (blockId: String) -> EditResult,
    onMove: (blockId: String, requestedStartMinutes: Int, window: IntRange) -> EditResult,
    onSeed: () -> Unit,
    onInviteClick: () -> Unit,
) {
    val list = blocks.orEmpty()
    val dates = remember(tripStart, tripEnd) { tripDates(tripStart, tripEnd) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedDate = dates.getOrNull(selectedIndex) ?: dates.first()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var choosingKind by remember { mutableStateOf(false) }
    var voting by remember { mutableStateOf<Block?>(null) }
    var sendingVote by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            MemberBar(members, onInviteClick)
            HorizontalDivider()
            DayTabs(dates, selectedIndex) { selectedIndex = it }

            val dayBlocks = list.filter { it.date == selectedDate }.sortedBy { it.start }
            DayTimelineGrid(
                dayBlocks = dayBlocks,
                markers = markers.filter { it.date == selectedDate },
                now = now,
                onBlockClick = { editor = EditorState(it, isNew = false) },
                onBlockMoved = { block, requested, window ->
                    when (val r = onMove(block.id, requested, window)) {
                        is EditResult.Ok -> {
                            val moved = r.blocks.firstOrNull { it.id == block.id }
                            if (block.kind == BlockKind.FIXED && block.status == BlockStatus.CONFIRMED &&
                                moved?.status == BlockStatus.PROPOSED
                            ) toast("확정 블록을 옮겨서 '제안됨'으로 바뀌었어요.")
                        }
                        is EditResult.Error -> toast(r.message)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        // 불러오는 중 / 여행 전체가 비어 있을 때 (Firestore 처음 연결 시 테스트용 버튼)
        if (blocks == null) {
            Text(
                "일정을 불러오는 중…", fontSize = 13.sp, color = TimelineColors.SubText,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (blocks.isEmpty()) {
            TextButton(onClick = onSeed, modifier = Modifier.align(Alignment.Center)) {
                Text("테스트용 가짜 일정 넣기")
            }
        }

        FloatingActionButton(
            onClick = { choosingKind = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            shape = CircleShape,
        ) { Text("+", fontSize = 28.sp) }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (choosingKind) {
        KindChooserDialog(
            onDismiss = { choosingKind = false },
            onChoose = { kind ->
                choosingKind = false
                val draft = newDraft(list, tripId, selectedDate, kind)
                if (draft == null) {
                    toast("이 날에는 1시간짜리 빈 시간이 없어요.")
                } else {
                    editor = EditorState(draft, isNew = true)
                }
            },
        )
    }

    editor?.let { st ->
        BlockEditorSheet(
            original = st.block,
            isNew = st.isNew,
            allBlocks = list,
            onDismiss = { editor = null },
            onSave = { edited ->
                val r = if (st.isNew) onAdd(edited) else onUpdate(st.block, edited)
                when (r) {
                    is EditResult.Ok -> { editor = null }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onDelete = {
                when (val r = onDelete(st.block.id)) {
                    is EditResult.Ok -> { editor = null; toast("블록을 삭제했어요.") }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onVoteClick = {
                editor = null
                voting = st.block
            },
        )
    }

    voting?.let { block ->
        VoteDialog(
            block = block,
            sending = sendingVote,
            onDismiss = { voting = null },
            onSubmit = { choice, comment ->
                scope.launch {
                    sendingVote = true
                    val result = voteActions.submitVote(tripId, block.id, choice, comment)
                    sendingVote = false
                    result
                        .onSuccess { voting = null; snackbar.showSnackbar("의견을 보냈어요.") }
                        .onFailure { snackbar.showSnackbar("보내지 못했어요: ${it.message}") }
                }
            },
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

/* ───────────────────────── 날짜 탭 (1일차, 2일차 …) ───────────────────────── */

@Composable
private fun DayTabs(dates: List<LocalDate>, selected: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected.coerceIn(0, dates.lastIndex),
        edgePadding = 12.dp,
        containerColor = Color.White,
        contentColor = Color.Black,
    ) {
        dates.forEachIndexed { i, date ->
            val isSel = i == selected
            Tab(
                selected = isSel,
                onClick = { onSelect(i) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${i + 1}일차",
                            fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else TimelineColors.SubText,
                        )
                        Text(date.format(MD), fontSize = 11.sp, color = TimelineColors.SubText)
                    }
                },
            )
        }
    }
}

/* ───────────────────────── + 버튼 → 종류 선택 ───────────────────────── */

@Composable
private fun KindChooserDialog(onDismiss: () -> Unit, onChoose: (BlockKind) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("어떤 블록을 추가할까요?", fontSize = 18.sp) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KindOption("확정 블록", "시간·장소·할 일을 정해요\n'제안됨'으로 시작", TimelineColors.Fixed, Modifier.weight(1f)) {
                    onChoose(BlockKind.FIXED)
                }
                KindOption("자유 블록", "메모만 남겨요\n비워두면 빈칸", TimelineColors.Free, Modifier.weight(1f)) {
                    onChoose(BlockKind.FREE)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun KindOption(title: String, desc: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, TimelineColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
        Text(desc, fontSize = 12.sp, color = Color(0xFF424242))
    }
}

/* ───────────────────────── Preview ───────────────────────── */

@Preview(showBackground = true, widthDp = 360, heightDp = 760, name = "1일차 · 현재 14:10")
@Composable
private fun TimelinePreview() {
    TimelineContent(
        blocks = FakeBlocks.blocks,
        tripId = FakeBlocks.TRIP_ID,
        members = FakeBlocks.members,
        tripStart = FakeBlocks.TRIP_START,
        tripEnd = FakeBlocks.TRIP_END,
        markers = FakeBlocks.markers,
        now = LocalTime.of(14, 10),
        snackbar = remember { SnackbarHostState() },
        voteActions = FakeVoteActions,
        onAdd = { EditResult.Ok(FakeBlocks.blocks) },
        onUpdate = { _, _ -> EditResult.Ok(FakeBlocks.blocks) },
        onDelete = { EditResult.Ok(FakeBlocks.blocks) },
        onMove = { _, _, _ -> EditResult.Ok(FakeBlocks.blocks) },
        onSeed = {},
        onInviteClick = {},
    )
}
