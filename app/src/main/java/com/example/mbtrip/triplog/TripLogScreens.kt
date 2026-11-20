package com.example.mbtrip.triplog

import android.view.LayoutInflater
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mbtrip.R
import com.example.mbtrip.timeline.model.Member
import com.example.mbtrip.timeline.ui.MemberProfileDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/* ───────────────────────── 사진 모아보기 (로그 목록) ───────────────────────── */

/**
 * res/layout/activity_trip_log_gallery.xml 을 띄우고 Firestore 로그와 잇는다.
 *  - 왼쪽 위 뒤로 가기 → onBack
 *  - 검색창(제목·내용·장소) + 오른쪽 달력 → 기간을 고르면 그 기간에 겹치는 여행의 로그만
 *  - 사진을 누르면 그 로그 상세(그 사진부터)로
 */
@Composable
fun TripLogGalleryScreen(
    onBack: () -> Unit,
    onOpenLog: () -> Unit,
    repository: TripLogRepository = TripLogDependencies.repository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember {
        mutableStateOf(
            TripLogStore.cachedLogs?.let { GalleryState.Ready(it, TripLogStore.cachedIsSample) } ?: GalleryState.Loading,
        )
    }
    var busy by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var rangeStart by rememberSaveable { mutableStateOf<String?>(null) }
    var rangeEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingRange by remember { mutableStateOf(false) }

    val range: LogDateRange? = remember(rangeStart, rangeEnd) {
        val s = rangeStart?.let { LocalDate.parse(it) }
        val e = rangeEnd?.let { LocalDate.parse(it) }
        if (s != null && e != null) LogDateRange(s, e) else null
    }

    fun setReady(all: List<TripLog>, sample: Boolean) {
        val logs = all.filterNot { it.key in TripLogStore.deletedKeys } // 지운 로그는 빼고
        state = GalleryState.Ready(logs, sample)
        TripLogStore.cachedLogs = logs
        TripLogStore.cachedIsSample = sample
    }

    fun load(showSpinner: Boolean) {
        scope.launch {
            if (showSpinner) state = GalleryState.Loading else busy = true
            runCatching { repository.loadLogs() }
                .onSuccess { setReady(it, sample = false) }
                .onFailure { e ->
                    // 화면에 이미 목록이 있으면 그대로 두고 알려만 준다
                    if (state is GalleryState.Ready) {
                        Toast.makeText(context, "새로 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                    } else {
                        state = GalleryState.Failed(e.message ?: "인터넷 연결을 확인해 주세요.")
                    }
                }
            busy = false
        }
    }

    fun seed() {
        scope.launch {
            busy = true
            runCatching { repository.seedSamples() }
                .onSuccess {
                                        load(showSpinner = false)
                }
                .onFailure { e ->
                    busy = false
                    Toast.makeText(context, "예시 로그를 만들지 못했어요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                }
        }
    }

    // 예시 로그는 테스트 계정만
    val tester = com.example.mbtrip.session.TestAccess.isTester

    // 들어올 때마다 새로 읽기 (테스트 계정이 예시를 보고 있던 중이면 그대로)
    LaunchedEffect(Unit) {
        if (!TripLogStore.cachedIsSample || !tester) load(showSpinner = state !is GalleryState.Ready || !tester)
    }

    val ready = state as? GalleryState.Ready
    val filtering = query.isNotBlank() || range != null
    val filtered = remember(ready, query, range) { ready?.logs.orEmpty().filterLogs(query, range) }

    val empty: EmptyInfo? = when (val s = state) {
        is GalleryState.Failed -> EmptyInfo(
            title = "로그를 불러오지 못했어요",
            desc = s.message,
            primary = "다시 시도" to { load(showSpinner = true) },
            secondary = if (tester) "예시로 보기" to { setReady(SampleTripLogs.logs, sample = true) } else null,
        )
        is GalleryState.Ready -> when {
            s.logs.isEmpty() && tester -> EmptyInfo(
                title = "아직 남긴 로그가 없어요",
                desc = "테스트 계정이에요. 예시 로그를 만들어 시나리오를 둘러볼 수 있어요.",
                primary = "예시 로그 만들기" to { seed() },
                secondary = "예시로 둘러보기" to { setReady(SampleTripLogs.logs, sample = true) },
            )
            s.logs.isEmpty() -> EmptyInfo(
                title = "아직 남긴 로그가 없어요",
                desc = "여행을 다녀와서 후기를 남기면 사진이 여기에 모여요.",
                primary = "새로고침" to { load(showSpinner = true) },
            )
            filtered.isEmpty() -> EmptyInfo(
                title = "조건에 맞는 로그가 없어요",
                desc = buildString {
                    if (query.isNotBlank()) append("'${query.trim()}' ")
                    range?.let { append(formatDateRange(it)) }
                    append("\n다른 검색어나 기간으로 찾아보세요.")
                }.trim(),
                primary = "검색 조건 지우기" to {
                    query = ""; rangeStart = null; rangeEnd = null
                    TripLogStore.galleryScroll = null
                },
            )
            else -> null
        }
        GalleryState.Loading -> null
    }

    // 상세로 갈 때만 목록을 기억해 두고, 화면을 아예 떠나면 다음에 DB 에서 새로 읽는다
    var keepCache by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (!keepCache) {
                TripLogStore.cachedLogs = null
                TripLogStore.cachedIsSample = false
                TripLogStore.galleryScroll = null
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.activity_trip_log_gallery, null, false)
            TripLogGalleryBinder(root, initialQuery = query).also { b ->
                b.restoreScrollLater(TripLogStore.galleryScroll)
                TripLogStore.galleryScroll = null
                root.tag = b
            }.root
        },
        update = { root ->
            val b = root.tag as TripLogGalleryBinder
            b.onBack = onBack
            b.onQueryChange = { query = it }
            b.onCalendarClick = { pickingRange = true }
            b.onClearPeriod = { rangeStart = null; rangeEnd = null }
            b.onSummaryAction = { seed() }
            b.onPhotoClick = { log, index ->
                keepCache = true
                TripLogStore.select(log, index)
                TripLogStore.galleryScroll = b.saveScroll()
                onOpenLog()
            }
            // "검색 조건 지우기" 로 비웠을 때 글자칸도 비우기
            if (query.isEmpty()) b.clearQueryIfNeeded()
            b.render(state, filtered, range, filtering, busy, empty)
        },
        modifier = Modifier.fillMaxSize(),
    )

    if (pickingRange) {
        LogDateRangeDialog(
            initial = range,
            onDismiss = { pickingRange = false },
            onConfirm = { picked ->
                pickingRange = false
                rangeStart = picked.start.toString()
                rangeEnd = picked.end.toString()
            },
        )
    }
}

/** 기간 고르기 — 앱 공용 달력 모달. 시작일만 고르면 그 하루 */
@Composable
private fun LogDateRangeDialog(
    initial: LogDateRange?,
    onDismiss: () -> Unit,
    onConfirm: (LogDateRange) -> Unit,
) {
    com.example.mbtrip.ui.design.MBDateRangeSheet(
        title = "여행 기간으로 로그 찾기",
        initialStart = initial?.start,
        initialEnd = initial?.end,
        onDismiss = onDismiss,
        onConfirm = { start, end -> onConfirm(LogDateRange(start, end)) },
        confirmText = "적용",
        startLabel = "시작",
        endLabel = "끝",
    )
}

@Suppress("unused")
private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
@Suppress("unused")
private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/* ───────────────────────── 후기 상세 ───────────────────────── */

/**
 * res/layout/activity_trip_log_detail.xml 을 띄운다.
 *  - 왼쪽 위 뒤로 가기 → onBack
 *  - 사진 여러 장: 옆으로 넘기는 슬라이드 + 아래 점
 *  - 오른쪽 아래 프로필 사진 → 그 사람 정보 모달 (일정표와 같은 MemberProfileDialog)
 * 목록에서 고른 로그(TripLogStore.selected)를 보여주고, 바로 열었으면 첫 로그를 불러온다.
 */
@Composable
fun TripLogDetailScreen(
    onBack: () -> Unit,
    /** 내가 쓴 후기의 [수정] 아이콘 → 후기 쓰기 화면(수정 모드) */
    onEdit: (TripLog) -> Unit = {},
    repository: TripLogRepository = TripLogDependencies.repository,
) {
    // 목록에서 고른 로그 (수정하고 돌아오면 바뀐 내용으로) — 바로 열었으면 아래에서 불러온 것
    var loaded by remember { mutableStateOf<TripLog?>(null) }
    val log = TripLogStore.selected ?: loaded
    val startIndex = remember { TripLogStore.selectedPhotoIndex }
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var viewing by remember { mutableStateOf<Member?>(null) }

    // 개발용 목록에서 바로 열었을 때: DB 의 첫 로그 → 없으면 예시
    LaunchedEffect(Unit) {
        if (log == null) {
            loaded = runCatching { repository.loadLogs().firstOrNull() }.getOrNull() ?: SampleTripLogs.logs.first()
        }
    }

    // 함께한 사람
    LaunchedEffect(log?.key) {
        val current = log ?: return@LaunchedEffect
        members = if (current.isSample) {
            SampleTripLogs.membersWithMe
        } else {
            runCatching { repository.loadMembers(current.tripId) }.getOrDefault(emptyList())
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    AndroidView(
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.activity_trip_log_detail, null, false)
            TripLogDetailBinder(root).also { root.tag = it }.root
        },
        update = { root ->
            val b = root.tag as TripLogDetailBinder
            b.onBack = onBack
            b.onMemberClick = { viewing = it }
            b.onEdit = { log?.let(onEdit) }
            b.onDelete = { confirmDelete = true }
            b.render(log, members, startIndex)
        },
        modifier = Modifier.fillMaxSize(),
    )
    }

    if (confirmDelete) {
        val target = log
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("이 여행 로그를 삭제할까요?") },
            text = { Text("사진과 글이 모두 지워지고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (target != null) {
                        scope.launch {
                            runCatching { repository.deleteLog(target) }
                                .onSuccess {
                                    TripLogStore.forget(target)
                                    Toast.makeText(context, "여행 로그를 삭제했어요.", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .onFailure { e ->
                                    Toast.makeText(context, "삭제하지 못했어요: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }) { Text("삭제", color = com.example.mbtrip.ui.theme.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }

    viewing?.let { m ->
        // 로그 화면에서는 보기만 (내보내기 버튼 없음)
        MemberProfileDialog(member = m, onDismiss = { viewing = null }, onRemove = null)
    }
}
