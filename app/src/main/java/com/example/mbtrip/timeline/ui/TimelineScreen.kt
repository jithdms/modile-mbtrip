package com.example.mbtrip.timeline.ui

import kotlinx.coroutines.tasks.await

import com.example.mbtrip.timeline.data.TripLive
import com.example.mbtrip.siheung.PendingRoutine
import com.example.mbtrip.siheung.PlaceLocator

import androidx.compose.ui.graphics.SolidColor
import com.example.mbtrip.diagnosis.GroupRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import com.example.mbtrip.notifications.AppNotification
import com.example.mbtrip.notifications.NotificationType
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import com.example.mbtrip.notifications.NotificationDependencies
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.example.mbtrip.timeline.data.MemberRepository
import com.example.mbtrip.session.TripSession
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.enterPop
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua50
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua600
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Warning

private data class EditorState(val block: Block, val isNew: Boolean)

/**
 * P6 일정표 — 저장소(Fake 또는 Firestore)와 연결된 진입점. NavGraph 에서는 TimelineScreen() 만 부르면 된다.
 *
 * @param repository   블록 저장소. 기본값은 TimelineDependencies 에서 고른 것 (Firebase 설정 후 Firestore 로 전환)
 * @param voteActions  투표 저장 로직 (최재성 담당). 기본값은 가짜
 * @param markers      체크인·체크아웃 시각 (파란 선)
 * @param now          현재 시각 표시선 위치 (빨간 선). null 이면 표시하지 않음
 * @param memberRepository 동행자 저장소 (상단 프로필 줄 + 친구 초대 모달)
 * @param onInviteClick 상단 `+` 를 눌렀을 때 추가로 할 일. 친구 초대 모달은 이것과 상관없이 항상 뜬다
 * @param onHomeClick  오른쪽 위 집 버튼 → 메인(방 만들기). null 이면 버튼 숨김
 */
@Composable
fun TimelineScreen(
    tripId: String = FakeBlocks.TRIP_ID,
    repository: BlockRepository = TimelineDependencies.blockRepository,
    voteActions: VoteActions = TimelineDependencies.voteActions,
    memberRepository: MemberRepository = TimelineDependencies.memberRepository,
    tripStart: LocalDate = FakeBlocks.TRIP_START,
    tripEnd: LocalDate = FakeBlocks.TRIP_END,
    markers: List<TimeMarker> = FakeBlocks.markers,
    now: LocalTime? = null,
    onInviteClick: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null,
    /** 오른쪽 위 🔔 → 알림 모아보기. null 이면 버튼 숨김 */
    onNotificationsClick: (() -> Unit)? = null,
    /** 여행이 끝났을 때 아래 [후기 쓰러 가기] → 후기 쓰기 화면. null 이면 버튼 숨김 */
    onWriteReview: (() -> Unit)? = null,
    /** [테스트용 가짜 일정 넣기] 버튼을 보일지 (테스트 계정만) */
    canSeedTestData: Boolean = false,
) {
    val unreadNotifications by remember { NotificationDependencies.repository.unreadCount() }
        .collectAsState(initial = 0)
    val members by remember(memberRepository, tripId) { memberRepository.observeMembers(tripId) }
        .collectAsState(initial = emptyList())
    val screenScope = rememberCoroutineScope()

    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext

    // trips/{tripId} 문서가 아직 없을 때만 만든다 (친구가 바꾼 기간을 덮어쓰지 않게)
    LaunchedEffect(tripId) {
        runCatching { TripLive.ensureTrip(tripId, TripSession.destination, tripStart, tripEnd) }
    }
    // 여행 기간은 실시간으로 — 다른 사람이 숙소 날짜로 기간을 넓히면 이 폰의 날짜 탭도 바로 늘어난다
    val live by remember(tripId) { TripLive.observeTrip(tripId) }.collectAsState(initial = null)
    val start = live?.start ?: tripStart
    val end = (live?.end ?: tripEnd).let { if (it.isBefore(start)) start else it }

    // 지금 같이 보고 있는 사람 (나 포함)
    val viewers by remember(tripId) { TripLive.observeViewers(tripId) }.collectAsState(initial = emptyList())
    LaunchedEffect(tripId) {
        TripLive.stayPresent(tripId, com.example.mbtrip.session.Account.profile?.nickname ?: "친구")
    }

    val vm: TimelineViewModel = viewModel(
        key = "timeline-$tripId",
        factory = TimelineViewModel.factory(repository, tripId),
    )
    val blocks by vm.blocks.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // 방 만들기에서 고른 추천 투어 루틴 → 처음 열 때 이 순서대로 블록을 넣는다
    val loaded = blocks != null
    LaunchedEffect(vm, loaded) {
        if (!loaded) return@LaunchedEffect
        PendingRoutine.take(tripId)?.let { routine ->
            vm.applyRoutine(routine, start, end) { p -> PlaceLocator.locate(appContext, p) }
        }
    }

    Box(Modifier.fillMaxSize()) {
    TimelineContent(
        blocks = blocks,
        tripId = tripId,
        members = members,
        tripStart = start,
        tripEnd = end,
        markers = markers,
        now = now,
        snackbar = snackbar,
        voteActions = voteActions,
        onAdd = vm::add,
        onUpdate = vm::update,
        onDelete = vm::delete,
        onMove = vm::move,
        // 테스트 계정만: 가짜 일정과 함께 가짜 동행자 3명도 members 에 넣는다
        onSeed = if (canSeedTestData) {
            {
                vm.seedFakeData(tripStart, tripEnd)
                screenScope.launch { runCatching { memberRepository.invite(tripId, FakeBlocks.members) } }
            }
        } else null,
        onToggleTodo = vm::toggleTodo,
        onRequestDelete = vm::markDeleteRequested,
        onProposeEdit = vm::proposeEdit,
        onCancelDeleteRequest = vm::cancelDeleteRequest,
        onDeleteProposed = vm::deleteProposed,
        onInviteClick = onInviteClick ?: {},
        onHomeClick = onHomeClick,
        onNotificationsClick = onNotificationsClick,
        unreadNotifications = unreadNotifications,
        onInvited = { added ->
            screenScope.launch { snackbar.showSnackbar(added.joinToString(", ") { it.nickname } + " 님을 초대했어요.") }
        },
        memberRepository = memberRepository,
        onRemoveMember = { m ->
            screenScope.launch {
                runCatching { memberRepository.remove(tripId, m.userId) }
                    .onSuccess { snackbar.showSnackbar("${m.nickname} 님을 내보냈어요.") }
                    .onFailure { snackbar.showSnackbar("내보내지 못했어요: ${it.message}") }
            }
        },
    )
        // 왼쪽 아래: (위) 함께 보는 사람 · (아래) 여행이 끝났으면 후기 쓰기
        val others = viewers.filter { it.uid != com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
        // 오는 날이 지났고 내가 아직 후기를 안 썼으면 버튼을 보인다
        val ended = end.isBefore(java.time.LocalDate.now())
        val reviewWritten by androidx.compose.runtime.produceState<Boolean?>(null, tripId, ended) {
            value = if (!ended) true else runCatching {
                val me = com.example.mbtrip.timeline.data.ensureSignedIn()
                me != null && com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("trips").document(tripId).collection("reviews").document(me)
                    .get().await().exists()
            }.getOrDefault(false)
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 88.dp, bottom = 20.dp), // 오른쪽은 + 버튼 자리
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = others.isNotEmpty(),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
                exit = androidx.compose.animation.fadeOut(),
            ) {
                LiveViewersPill(others.map { it.nickname })
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = onWriteReview != null && ended && reviewWritten == false &&
                    tripId !in com.example.mbtrip.triplog.ReviewReminders.savedTrips, // 방금 썼으면 숨김
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
                exit = androidx.compose.animation.fadeOut(),
            ) {
                WriteReviewPill { onWriteReview?.invoke() }
            }
        }

        // 방장이 이 방을 지웠으면 → 안내 + 메인으로 (내 여행 목록에서도 뺀다)
        if (live?.deleted == true) {
            LaunchedEffect(tripId) { com.example.mbtrip.session.MyTrips.forget(tripId) }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.97f))
                    .clickable(enabled = true, onClick = {}), // 뒤 일정표를 못 누르게
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    com.example.mbtrip.ui.design.MBIcon(com.example.mbtrip.R.drawable.ic_mb_delete, size = 36.dp, tint = TimelineColors.SubText)
                    Spacer(Modifier.height(12.dp))
                    Text("삭제된 방이에요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("방을 만든 사람이 이 여행 방을 삭제했어요.", fontSize = 13.sp, color = TimelineColors.SubText)
                    Spacer(Modifier.height(20.dp))
                    com.example.mbtrip.ui.design.MBButton(
                        text = "메인으로",
                        onClick = { onHomeClick?.invoke() },
                        style = com.example.mbtrip.ui.design.MBButtonStyle.Deep,
                        height = 48.dp,
                    )
                }
            }
        }
    }
}

/** 여행이 끝났어요 · 후기 쓰러 가기 → */
@Composable
private fun WriteReviewPill(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Ink)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.example.mbtrip.ui.design.MBIcon(com.example.mbtrip.R.drawable.ic_mb_edit, size = 16.dp, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text("여행이 끝났어요 · 후기 쓰러 가기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.width(4.dp))
        com.example.mbtrip.ui.design.MBIcon(com.example.mbtrip.R.drawable.ic_mb_chevron_right, size = 16.dp, tint = Color.White)
    }
}

/** ● 민지 님이 함께 보는 중 */
@Composable
private fun LiveViewersPill(names: List<String>) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Line, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(Color(0xFF2FB36B), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            when (names.size) {
                1 -> "${names[0]} 님이 함께 보는 중"
                else -> "${names[0]} 님 외 ${names.size - 1}명이 함께 보는 중"
            },
            fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium,
        )
    }
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
    /** null 이면 [테스트용 가짜 일정 넣기] 버튼을 숨긴다 (테스트 계정이 아님) */
    onSeed: (() -> Unit)?,
    onInviteClick: () -> Unit,
    onToggleTodo: (blockId: String, lineIndex: Int) -> Unit = { _, _ -> },
    onRequestDelete: (blockId: String) -> EditResult = { EditResult.Error("삭제 제안을 할 수 없어요.") },
    onProposeEdit: (original: Block, edited: Block) -> EditResult = { _, _ -> EditResult.Error("수정 제안을 할 수 없어요.") },
    onCancelDeleteRequest: (blockId: String) -> EditResult = { EditResult.Error("취소할 수 없어요.") },
    onDeleteProposed: (blockId: String) -> EditResult = { EditResult.Error("삭제할 수 없어요.") },
    onHomeClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    unreadNotifications: Int = 0,
    onRemoveMember: ((Member) -> Unit)? = null,
    memberRepository: MemberRepository = TimelineDependencies.memberRepository,
    onInvited: ((List<Member>) -> Unit)? = null,
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

    // 일정표를 맨 위까지 올리면 프로필 크게, 내리면 기본 크기
    val gridScroll = rememberScrollState()
    val atTop by remember { derivedStateOf { gridScroll.value < 24 } }
    val avatarSize by animateDpAsState(if (atTop) 56.dp else 44.dp, label = "avatarSize")
    LaunchedEffect(selectedIndex) { gridScroll.scrollTo(0) } // 날짜 바꾸면 맨 위부터

    // 알림에서 "이 블록 보여줘" → 그 날짜로 바꾸고 3.5초 동안 강조
    val focusRequest by TimelineFocus.request.collectAsState()
    var highlightId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusRequest, list) {
        val id = focusRequest ?: return@LaunchedEffect
        val target = list.firstOrNull { it.id == id } ?: return@LaunchedEffect // 블록이 아직 안 불러와졌으면 기다림
        val dayIndex = dates.indexOf(target.date)
        if (dayIndex >= 0) selectedIndex = dayIndex
        highlightId = id
        TimelineFocus.consume()
        delay(3500)
        highlightId = null
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            // 위쪽(동행자 · 날짜 탭)은 은은한 하늘 배경
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                Box(Modifier.enterFromBelow(0, distance = 10.dp)) {
                    MemberBar(
                        members, onInviteClick, onHomeClick, onRemoveMember, tripId, memberRepository, onInvited, avatarSize,
                        onNotificationsClick, unreadNotifications,
                    )
                }
                Box(Modifier.enterFromBelow(1, distance = 10.dp)) {
                    DayTabs(dates, selectedIndex) { selectedIndex = it }
                }
            }
            // 일차 탭 ↔ 비율 구분선
            HorizontalDivider(color = Line)

            val dayBlocks = list.filter { it.date == selectedDate }.sortedBy { it.start }
            KindRatioBar(dayBlocks, list, GroupRatio.of(members.map { it.jpScore }))
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
                scrollState = gridScroll,
                highlightId = highlightId,
                // 카드 안 할 일 동그라미 → 그 줄만 [ ] ↔ [x]
                onToggleTodo = { block, line -> onToggleTodo(block.id, line) },
            )
        }

        // 불러오는 중 / 여행 전체가 비어 있을 때 (Firestore 처음 연결 시 테스트용 버튼)
        if (blocks == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(28.dp), color = Aqua500, strokeWidth = 3.dp)
                Spacer(Modifier.height(10.dp))
                Text("일정을 불러오는 중…", fontSize = 13.sp, color = TimelineColors.SubText)
            }
        } else if (blocks.isEmpty()) {
            Column(
                Modifier.align(Alignment.Center).enterPop(0, from = 0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                com.example.mbtrip.ui.design.MBIcon(com.example.mbtrip.R.drawable.ic_mb_calendar, size = 36.dp, tint = TimelineColors.SubText)
                Spacer(Modifier.height(6.dp))
                if (onSeed != null) {
                    TextButton(onClick = onSeed) {
                        Text("테스트용 가짜 일정 넣기", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("아직 일정이 없어요", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(2.dp))
                    Text("오른쪽 아래 + 버튼으로 첫 블록을 추가해 보세요.", fontSize = 13.sp, color = TimelineColors.SubText)
                }
            }
        }

        AddFab(
            open = choosingKind,
            onClick = { choosingKind = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )

        // 알림 줄 — 남색 알약, 아래에서 떠오른다
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Ink,
                contentColor = Color.White,
                actionColor = Aqua300,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(horizontal = 16.dp).enterPop(0, from = 0.85f),
            )
        }
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
            onToggleTodo = { line -> onToggleTodo(st.block.id, line) },
            onProposeDelete = { reason ->
                when (val r = onRequestDelete(st.block.id)) {
                    is EditResult.Ok -> {
                        editor = null
                        postProposalNotification(NotificationType.DELETE_PROPOSAL, "삭제 제안", "'${st.block.displayTitle}' 삭제가 제안됐어요 · \"$reason\"", st.block)
                        scope.launch {
                            voteActions.proposeDelete(tripId, st.block.id, reason)
                                .onSuccess { snackbar.showSnackbar("삭제를 제안했어요. 동행자 의견을 기다려요.") }
                                .onFailure { snackbar.showSnackbar("삭제 제안을 보내지 못했어요: ${it.message}") }
                        }
                    }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onProposeEdit = { edited ->
                val before = st.block
                when (val r = onProposeEdit(before, edited)) {
                    is EditResult.Ok -> {
                        editor = null
                        val summary = editSummary(before, edited)
                        postProposalNotification(NotificationType.VOTE_REQUEST, "수정 제안 · 투표해 주세요", "'${edited.displayTitle}' $summary", edited)
                        scope.launch {
                            voteActions.proposeEdit(tripId, before.id, summary)
                                .onSuccess { snackbar.showSnackbar("수정을 제안했어요. 다시 투표에 올라갔어요.") }
                                .onFailure { snackbar.showSnackbar("수정 제안을 보내지 못했어요: ${it.message}") }
                        }
                    }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onCancelDeleteRequest = {
                when (val r = onCancelDeleteRequest(st.block.id)) {
                    is EditResult.Ok -> { editor = null; toast("삭제 제안을 취소했어요.") }
                    is EditResult.Error -> toast(r.message)
                }
            },
            onDeleteProposed = {
                when (val r = onDeleteProposed(st.block.id)) {
                    is EditResult.Ok -> { editor = null; toast("'${st.block.displayTitle}' 일정을 삭제했어요.") }
                    is EditResult.Error -> toast(r.message)
                }
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

/** 맨 위: [+](프로필 사진들) ………… ⌂ 메인으로 (onHomeClick 이 null 이면 집 버튼 숨김) */
@Composable
private fun MemberBar(
    members: List<Member>,
    onInviteClick: () -> Unit,
    onHomeClick: (() -> Unit)?,
    onRemoveMember: ((Member) -> Unit)?,
    tripId: String,
    memberRepository: MemberRepository,
    onInvited: ((List<Member>) -> Unit)?,
    avatarSize: Dp,
    onNotificationsClick: (() -> Unit)?,
    unreadNotifications: Int,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberRow(
            members, onInviteClick, Modifier.weight(1f),
            onRemoveMember = onRemoveMember,
            tripId = tripId,
            memberRepository = memberRepository,
            onInvited = onInvited,
            avatarSize = avatarSize,
        )
        if (onNotificationsClick != null) BellButton(onNotificationsClick, unread = unreadNotifications)
        if (onHomeClick != null) HomeButton(onHomeClick)
    }
}

/* ───────────────────────── 날짜 탭 (1일차, 2일차 …) ───────────────────────── */

@Composable
private fun DayTabs(dates: List<LocalDate>, selected: Int, onSelect: (Int) -> Unit) {
    val sel = selected.coerceIn(0, dates.lastIndex)
    ScrollableTabRow(
        selectedTabIndex = sel,
        edgePadding = 12.dp,
        containerColor = Color.Transparent,
        contentColor = Ink,
        divider = {},
        // 고른 날짜 아래 남색 막대가 미끄러져 따라간다
        indicator = { positions ->
            if (sel < positions.size) {
                Box(
                    Modifier
                        .tabIndicatorOffset(positions[sel])
                        .padding(horizontal = 20.dp)
                        .height(2.dp)
                        .background(Ink),
                )
            }
        },
    ) {
        dates.forEachIndexed { i, date ->
            val isSel = i == selected
            val color by animateColorAsState(if (isSel) Ink else TimelineColors.SubText, label = "tabColor")
            Tab(
                selected = isSel,
                onClick = { onSelect(i) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "${i + 1}일차",
                            fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = color,
                        )
                        Text(date.format(MD), fontSize = 11.sp, color = if (isSel) Aqua700 else TimelineColors.SubText)
                    }
                },
            )
        }
    }
}

/** 오른쪽 아래 + 버튼 — 남색 동그라미, 열리면 × 로 돈다 */
@Composable
private fun AddFab(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (open) 45f else 0f, tween(220), label = "fabTurn")
    Box(
        modifier
            .size(56.dp)
            .bouncyClick(CircleShape, onClick = onClick)
            .background(Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        com.example.mbtrip.ui.design.MBIcon(
            com.example.mbtrip.R.drawable.ic_mb_add, size = 26.dp, tint = Color.White,
            contentDescription = "블록 추가", modifier = Modifier.rotate(turn),
        )
    }
}

/* ───────────────────────── + 버튼 → 종류 선택 ───────────────────────── */

@Composable
private fun KindChooserDialog(onDismiss: () -> Unit, onChoose: (BlockKind) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { Text("어떤 블록을 추가할까요?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            // 가로로 꽉 찬 버튼을 위아래로 (차례로 떠오른다)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KindOption(com.example.mbtrip.R.drawable.ic_mb_pin, "확정 블록", "시간·장소·할 일을 정해요 · '제안됨'으로 시작", TimelineColors.Fixed, Modifier.fillMaxWidth().enterFromBelow(0)) {
                    onChoose(BlockKind.FIXED)
                }
                KindOption(com.example.mbtrip.R.drawable.ic_mb_cafe, "자유 블록", "메모만 남겨요 · 비워두면 빈칸", TimelineColors.Free, Modifier.fillMaxWidth().enterFromBelow(1)) {
                    onChoose(BlockKind.FREE)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun KindOption(icon: Int, title: String, desc: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .bouncyClick(shape, scaleDown = 0.96f, elevation = 4.dp, shadowColor = Aqua500, onClick = onClick)
            .background(lerp(color, Color.White, 0.55f), shape)
            .border(1.dp, lerp(color, Color.Black, 0.12f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(Color.White, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            com.example.mbtrip.ui.design.MBIcon(icon, size = 20.dp, tint = Ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            Text(desc, fontSize = 12.sp, color = InkSoft)
        }
        com.example.mbtrip.ui.design.MBIcon(com.example.mbtrip.R.drawable.ic_mb_arrow_right, size = 18.dp, tint = Ink.copy(alpha = 0.5f))
    }
}

/* ───────────────────────── 제안 알림 ───────────────────────── */

/** 삭제·수정 제안을 알림 모아보기에도 남긴다 (지금은 이 폰에만. Firestore 알림은 다음 단계) */
private fun postProposalNotification(type: NotificationType, title: String, body: String, block: Block) {
    NotificationDependencies.repository.post(
        AppNotification(
            id = "p_${block.id}_${System.currentTimeMillis()}",
            type = type,
            title = title,
            body = body,
            createdAt = LocalDateTime.now(),
            tripId = block.tripId,
            blockId = block.id,
        ),
    )
}

/** 무엇을 바꿨는지 한 줄: "시간 10:30~12:30 → 11:00~13:00 · 장소 A → B" */
private fun editSummary(before: Block, after: Block): String {
    val parts = buildList {
        if (before.start != after.start || before.end != after.end) add("시간 ${before.timeText()} → ${after.timeText()}")
        if (before.place != after.place) add("장소 ${before.place ?: "없음"} → ${after.place ?: "없음"}")
        if (before.title != after.title) add("제목 변경")
        if (before.memo != after.memo || before.todo != after.todo) add("메모·할 일 변경")
        if (before.tags != after.tags) add("태그 변경")
    }
    return if (parts.isEmpty()) "내용 수정 제안" else parts.joinToString(" · ")
}

/* ───────────────────────── 확정 · 자유 비율 ───────────────────────── */

/**
 * 날짜 탭 아래: 이 날 확정 블록과 자유 블록이 시간으로 몇 % 씩인지. 오른쪽 아래 ▾/▴ 로 접고 펴기.
 *
 *  펼침:  ● 확정 62%  ● 자유 38%          여행 전체 확정 55%
 *         [■■■■■■■■■■■■□□□□□□□]
 *                                              ▴
 *  접힘:  확정 62% · 자유 38%                  ▾
 *
 * 이동 블록은 계산에서 뺀다.
 */
@Composable
private fun KindRatioBar(dayBlocks: List<Block>, allBlocks: List<Block>, recommended: GroupRatio? = null) {
    fun minutesOf(bs: List<Block>, k: BlockKind) = bs.filter { it.kind == k }.sumOf { it.durationMinutes }
    val fixed = minutesOf(dayBlocks, BlockKind.FIXED)
    val free = minutesOf(dayBlocks, BlockKind.FREE)
    val total = fixed + free
    val tripFixed = minutesOf(allBlocks, BlockKind.FIXED)
    val tripTotal = tripFixed + minutesOf(allBlocks, BlockKind.FREE)
    val fixedPct = if (total == 0) 0 else (fixed * 100f / total).roundToInt()
    val freePct = if (total == 0) 0 else 100 - fixedPct

    var expanded by rememberSaveable { mutableStateOf(true) }
    val arrowTurn by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    Column(
        Modifier
            .fillMaxWidth()
            .background(Aqua50)
            .animateContentSize()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
    ) {
        if (expanded) {
            if (total == 0) {
                Text("이 날은 아직 확정·자유 블록이 없어요.", fontSize = 12.sp, color = TimelineColors.SubText)
            } else {
                val animated by animateFloatAsState(fixed.toFloat() / total, label = "ratio")
                Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RatioLegend(TimelineColors.Fixed, "확정 $fixedPct%")
                    Spacer(Modifier.width(10.dp))
                    RatioLegend(TimelineColors.Free, "자유 $freePct%")
                    Spacer(Modifier.weight(1f))
                    if (tripTotal > 0) {
                        Text(
                            "여행 전체 확정 ${(tripFixed * 100f / tripTotal).roundToInt()}%",
                            fontSize = 11.sp, color = TimelineColors.SubText,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.padding(end = 8.dp).fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TimelineColors.Free),
                    ) {
                        if (animated > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(animated.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(TimelineColors.FixedDash),
                            )
                        }
                    }
                    // 적정 비율 위치 = 검은 세로 눈금
                    if (recommended != null) {
                        BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp)) {
                            val x = (maxWidth - 2.dp) * (recommended.fixedPercent / 100f)
                            Box(
                                Modifier
                                    .offset(x = x)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Ink),
                            )
                        }
                    }
                }
                if (recommended != null) {
                    val diff = fixedPct - recommended.fixedPercent
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "▎적정 확정 ${recommended.fixedPercent}% · 자유 ${recommended.freePercent}%  " + when {
                            diff >= 10 -> "· 확정이 조금 많아요"
                            diff <= -10 -> "· 자유가 조금 많아요"
                            else -> "· 딱 좋아요"
                        },
                        fontSize = 11.sp,
                        color = if (kotlin.math.abs(diff) >= 10) Warning else TimelineColors.SubText,
                    )
                }
            }
        }

        // 오른쪽 아래 화살표 (접혀 있을 땐 왼쪽에 한 줄 요약)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!expanded) {
                Text(
                    if (total == 0) "확정·자유 블록 없음" else "확정 $fixedPct% · 자유 $freePct%",
                    fontSize = 12.sp, color = TimelineColors.SubText,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "▾",
                    fontSize = 16.sp,
                    color = TimelineColors.SubText,
                    modifier = Modifier.rotate(arrowTurn),
                )
            }
        }
    }
    // 아래 일정표와 확실히 구분
    HorizontalDivider(color = Line)
}

@Composable
private fun RatioLegend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color).border(1.dp, TimelineColors.Border, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
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
        onHomeClick = {},
    )
}
