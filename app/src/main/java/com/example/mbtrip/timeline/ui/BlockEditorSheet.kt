package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Transport
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private val TAG_OPTIONS = listOf("맛집", "카페", "명소", "액티비티", "쇼핑")

/* 와이어프레임 '방 생성 페이지2' 모달 색 */
private val ModalBg = Color(0xFFF4F4F4)
private val FieldBorder = Color(0xFF555555)
private val Black = Color(0xFF1E1E1E)
private val TagOn = Color(0xFFA9E5A0)       // 선택된 태그 = 자유 블록 초록
private val TagOnBorder = Color(0xFF4F9A47)
private val FieldShape = RoundedCornerShape(14.dp)

/**
 * 블록 추가·수정 모달 (블록 한 번 누르기 / + 버튼) — 화면 가운데 뜨는 카드.
 *
 *  - 확정: 태그 → 지도 → 장소 → 시간 → 메모 (+ 더보기: 제목·할 일)   ← 와이어프레임 '방 생성 페이지2'
 *  - 자유: 시간, 메모
 *  - 이동: 시간, 이동수단, 메모
 *  - 맨 위 [자유 | 확정] 으로 서로 전환 (이동은 전환 불가)
 *
 * 장소: 가게·장소 이름을 적고 [검색] → 결과 목록에서 고르면 핀·상세 주소가 채워진다.
 *       지도를 직접 눌러도 핀이 옮겨지고 그 위치의 상세 주소가 나온다.
 * 이전 일정: 같은 날 바로 전 일정(위치 있는 것)과의 직선거리·대략 이동 시간을 보여준다.
 * 제목: 비워두면 장소 이름이 카드 제목이 된다.
 */
@Composable
internal fun BlockEditorSheet(
    original: Block,
    isNew: Boolean,
    allBlocks: List<Block>,
    onDismiss: () -> Unit,
    onSave: (Block) -> Unit,
    onDelete: () -> Unit,
    onVoteClick: () -> Unit,
    /** 잠긴 블록의 할 일 동그라미 → 바로 저장 (잠긴 블록은 [저장하기] 가 막혀 있어서) */
    onToggleTodo: ((lineIndex: Int) -> Unit)? = null,
    /** 확정·잠긴 블록 '삭제 제안' (사유). null 이면 버튼 숨김 */
    onProposeDelete: ((reason: String) -> Unit)? = null,
    /** 확정·잠긴 블록 '수정 제안' (고친 블록). null 이면 수정 제안 불가 */
    onProposeEdit: ((Block) -> Unit)? = null,
    /** 삭제 제안 중인 블록: 제안 취소 / 실제로 삭제 */
    onCancelDeleteRequest: (() -> Unit)? = null,
    onDeleteProposed: (() -> Unit)? = null,
) {
    var draft by remember(original.id, isNew) { mutableStateOf(original) }
    var confirmDelete by remember { mutableStateOf(false) }
    var proposingDelete by remember { mutableStateOf(false) }
    var choosingProposal by remember { mutableStateOf(false) }   // [삭제/수정 제안하기] → 고르기
    var editingProposal by remember { mutableStateOf(false) }    // 수정 제안 중 (확정 블록도 고칠 수 있게)
    var confirmProposedDelete by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(original.title.isNotBlank() && original.title != original.place || original.todo.isNotBlank()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }

    // 확정됐거나 잠긴 블록은 바로 못 고치고 '삭제/수정 제안' 을 거친다
    val needsProposal = !isNew && original.needsDeleteProposal()
    val editable = if (needsProposal) editingProposal else !original.locked
    val problem = remember(draft, allBlocks) { validate(allBlocks, draft) }
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun runSearch() {
        val query = draft.place.orEmpty().trim()
        focus.clearFocus()
        if (query.isEmpty()) {
            searchMessage = "장소 이름을 먼저 적어주세요."
            return
        }
        scope.launch {
            searching = true
            searchMessage = null
            results = emptyList()
            val found = vworldSearch(query)
            if (found.isNotEmpty()) {
                results = found
            } else {
                // 브이월드 검색이 안 되면 안드로이드 기본 검색으로 한 번 더
                val hit = searchPlace(context, query)
                if (hit == null) {
                    searchMessage = "시흥 안에서 '$query'을(를) 찾지 못했어요. 지도를 눌러 직접 찍어주세요."
                } else {
                    draft = draft.copy(lat = hit.lat, lng = hit.lng, address = vworldAddress(hit.lat, hit.lng) ?: draft.address)
                }
            }
            searching = false
        }
    }

    fun choose(r: PlaceResult) {
        draft = draft.copy(place = r.name, address = r.address.ifBlank { null }, lat = r.lat, lng = r.lng)
        results = emptyList()
        searchMessage = null
    }

    var mapFull by remember { mutableStateOf(false) }

    // 지도를 눌렀을 때 (작은 지도·전체 지도 공용): 핀 옮기고 그 위치 주소 채우기
    val pickAt: (Double, Double) -> Unit = { la, ln ->
        draft = draft.copy(lat = la, lng = ln, address = null)
        searchMessage = null
        results = emptyList()
        scope.launch {
            val addr = vworldAddress(la, ln) ?: reverseGeocode(context, la, ln)
            // 그 사이 다른 곳을 또 눌렀으면 무시
            if (draft.lat == la && draft.lng == ln) {
                draft = draft.copy(
                    address = addr,
                    place = if (draft.place.isNullOrBlank()) addr else draft.place,
                )
            }
        }
    }

    // 이미 일정에 넣은 다른 장소들 (지도에 이름표로)
    val spots = remember(allBlocks, draft.id) {
        allBlocks
            .filter { it.id != draft.id && it.hasLocation }
            .sortedWith(compareBy({ it.date }, { it.start }))
            .map { MapSpot(it.lat!!, it.lng!!, "${it.date.monthValue}/${it.date.dayOfMonth} ${it.displayTitle}") }
    }

    // 같은 날, 이 일정보다 먼저 끝나는 일정 중 위치가 있는 가장 가까운 것
    val prev = remember(draft.date, draft.start, draft.id, allBlocks) {
        allBlocks
            .filter { it.id != draft.id && it.date == draft.date && it.end <= draft.start && it.hasLocation }
            .maxByOrNull { it.end }
    }

    // 장소 이름만 있고 좌표가 없는 블록(예전 데이터)은 열 때 한 번 찾아서 핀을 꽂아준다
    LaunchedEffect(original.id) {
        val place = original.place
        if (original.kind == BlockKind.FIXED && !original.hasLocation && !place.isNullOrBlank()) {
            val r = vworldSearch(place).firstOrNull()
            if (r != null && !draft.hasLocation) {
                draft = draft.copy(lat = r.lat, lng = r.lng, address = draft.address ?: r.address.ifBlank { null })
            } else {
                val hit = searchPlace(context, place)
                if (hit != null && !draft.hasLocation) draft = draft.copy(lat = hit.lat, lng = hit.lng)
            }
        } else if (original.hasLocation && original.address.isNullOrBlank()) {
            // 좌표만 있고 주소가 없으면 채워준다
            val addr = vworldAddress(original.lat!!, original.lng!!)
            if (addr != null && draft.address.isNullOrBlank()) draft = draft.copy(address = addr)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .imePadding()
                .clip(RoundedCornerShape(20.dp))
                .background(ModalBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 0) 종류 전환 + 닫기
            Header(draft, editable, onKind = { draft = draft.withKind(it) }, onClose = onDismiss)
            StatusHint(original, draft, isNew, needsProposal, editingProposal)

            when (draft.kind) {
                BlockKind.FIXED -> {
                    // 1) 태그
                    TagPicker(draft.tags, editable) { draft = draft.copy(tags = it) }

                    // 2) 지도
                    Box(Modifier.fillMaxWidth().height(150.dp)) {
                        PlaceMap(
                            lat = draft.lat,
                            lng = draft.lng,
                            enabled = editable,
                            onPick = pickAt,
                            onOutside = { searchMessage = "시흥 안의 장소만 고를 수 있어요." },
                            prevLat = prev?.lat,
                            prevLng = prev?.lng,
                            spots = spots,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, FieldBorder, RoundedCornerShape(12.dp)),
                        )
                        // 전체 보기: 아이콘만
                        Text(
                            "⤢",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { mapFull = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        if (draft.hasLocation) "지도를 누르면 핀을 옮길 수 있어요." else "파란 테두리(시흥) 안의 장소를 검색하거나, 지도를 눌러 위치를 찍어주세요.",
                        fontSize = 11.sp, color = TimelineColors.SubText,
                    )

                    // 3) 장소
                    Section("장소") {
                        PlaceField(
                            value = draft.place.orEmpty(),
                            enabled = editable,
                            searching = searching,
                            onChange = { draft = draft.copy(place = it); searchMessage = null },
                            onSearch = { runSearch() },
                        )
                        searchMessage?.let { Text(it, fontSize = 12.sp, color = TimelineColors.Error) }
                        if (results.isNotEmpty()) {
                            SearchResults(results, onChoose = { choose(it) }, onClose = { results = emptyList() })
                        }
                        draft.address?.takeIf { it.isNotBlank() }?.let { AddressLine(it) }
                    }

                    // 이전 일정과의 거리
                    val cur = draft
                    if (prev != null && cur.lat != null && cur.lng != null) {
                        PrevDistanceCard(prev, cur.lat, cur.lng, cur.start)
                    }

                    // 4) 시간
                    Section("시간") { TimeField(draft, editable) { pickingTime = true } }

                    // 5) 메모
                    Section("메모") {
                        BoxField(draft.memo.orEmpty(), editable, minLines = 3, placeholder = "예) 밥 먹기") {
                            draft = draft.copy(memo = it)
                        }
                    }

                    // 6) 더보기: 제목 · 할 일
                    Text(
                        if (showMore) "제목·할 일 접기 ▴" else "제목·할 일 더보기 ▾",
                        fontSize = 13.sp, color = TimelineColors.SubText,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showMore = !showMore }.padding(4.dp),
                    )
                    if (showMore) {
                        Section("제목") {
                            BoxField(draft.title, editable, placeholder = "비워두면 장소 이름으로 보여요") {
                                draft = draft.copy(title = it)
                            }
                        }
                        // 잠긴 블록도 체크는 됨: 체크만 바로 저장하고 화면 값도 같이 바꾼다
                        TodoEditor(
                            md = draft.todo,
                            enabled = editable,
                            onToggleOnly = if (!editable && onToggleTodo != null) {
                                { line -> onToggleTodo(line); draft = draft.copy(todo = toggleCheck(draft.todo, line)) }
                            } else {
                                null
                            },
                        ) { draft = draft.copy(todo = it) }
                    }
                }

                BlockKind.FREE -> {
                    Section("시간") { TimeField(draft, editable) { pickingTime = true } }
                    Section("메모") {
                        BoxField(draft.memo.orEmpty(), editable, minLines = 4, placeholder = "비워두면 '빈칸'으로 보여요") {
                            draft = draft.copy(memo = it)
                        }
                    }
                }

                BlockKind.MOVE -> {
                    Section("시간") { TimeField(draft, editable) { pickingTime = true } }
                    TransportPicker(draft.transport, editable) { draft = draft.copy(transport = it) }
                    Section("메모") {
                        BoxField(draft.memo.orEmpty(), editable, minLines = 2, placeholder = "예) 서울역 3번 출구") {
                            draft = draft.copy(memo = it)
                        }
                    }
                }
            }

            // 안내 · 오류
            if (original.locked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LockIcon(TimelineColors.SubText, 12.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("잠긴 블록은 수정·이동·삭제할 수 없어요.", fontSize = 13.sp, color = TimelineColors.SubText)
                }
            }
            problem?.let { Text(it, fontSize = 13.sp, color = TimelineColors.Error) }

            // 큰 버튼
            //  - 보통 블록: 저장하기 / 추가하기
            //  - 확정·잠긴 블록: 삭제/수정 제안하기 → (수정 고르면) 수정 제안 보내기
            val canPropose = needsProposal && !original.deleteRequested && (onProposeDelete != null || onProposeEdit != null)
            when {
                needsProposal && editingProposal -> {
                    val canSend = problem == null && onProposeEdit != null && draft != original
                    BigButton("수정 제안 보내기", enabled = canSend) { onProposeEdit?.invoke(draft.withAutoTitle(original)) }
                    TextButton(
                        onClick = { draft = original; editingProposal = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("수정 취소", color = TimelineColors.SubText) }
                }
                needsProposal -> {
                    if (canPropose) BigButton("삭제/수정 제안하기", enabled = true) { choosingProposal = true }
                }
                else -> {
                    BigButton(if (isNew) "추가하기" else "저장하기", enabled = editable && problem == null) {
                        onSave(draft.withAutoTitle(original))
                    }
                }
            }

            // 삭제 제안 중
            if (!isNew && original.deleteRequested) {
                Text(
                    "삭제 제안 중이에요 · 동행자 의견을 기다리고 있어요.",
                    fontSize = 12.sp, color = TimelineColors.Error,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (onCancelDeleteRequest != null) TextButton(onClick = onCancelDeleteRequest) {
                        Text("제안 취소", color = Black)
                    }
                    if (onDeleteProposed != null) TextButton(onClick = { confirmProposedDelete = true }) {
                        Text("삭제하기", color = TimelineColors.Error)
                    }
                }
            }

            // 투표 · 바로 삭제 (확정 전 블록만)
            val canVote = !isNew && original.kind == BlockKind.FIXED && original.status != BlockStatus.CONFIRMED
            val canDelete = !isNew && !needsProposal && editable
            if ((canVote || canDelete) && !editingProposal) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (canVote) TextButton(onClick = onVoteClick) { Text("투표하기", color = Black) }
                    if (canDelete) TextButton(onClick = { confirmDelete = true }) {
                        Text("블록 삭제", color = TimelineColors.Error)
                    }
                }
            }
        }
    }

    if (mapFull) {
        FullMapDialog(
            draft = draft,
            prev = prev,
            editable = editable,
            message = searchMessage,
            onPick = pickAt,
            onOutside = { searchMessage = "시흥 안의 장소만 고를 수 있어요." },
            onChoose = { choose(it) },
            onClose = { mapFull = false },
            spots = spots,
        )
    }

    if (pickingTime) {
        TimeRangePickerDialog(
            initialStart = draft.start,
            initialEnd = draft.end,
            onDismiss = { pickingTime = false },
            onConfirm = { s, e ->
                draft = draft.copy(start = s, end = e)
                pickingTime = false
            },
        )
    }

    // [삭제/수정 제안하기] → 둘 중 고르기
    if (choosingProposal) {
        AlertDialog(
            onDismissRequest = { choosingProposal = false },
            title = { Text("어떤 제안을 할까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "'${original.displayTitle}' 은(는) " + (if (original.locked) "잠긴" else "확정된") +
                            " 일정이라 바로 바꿀 수 없어요. 동행자들에게 물어볼게요.",
                        fontSize = 13.sp, color = TimelineColors.SubText,
                    )
                    if (onProposeEdit != null) {
                        ProposalOption("✏️  수정 제안", "시간·장소·메모를 고쳐서 다시 투표에 올려요") {
                            choosingProposal = false
                            editingProposal = true
                        }
                    }
                    if (onProposeDelete != null) {
                        ProposalOption("🗑️  삭제 제안", "이 일정을 지울지 동행자에게 물어봐요", danger = true) {
                            choosingProposal = false
                            proposingDelete = true
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosingProposal = false }) { Text("취소") } },
        )
    }

    if (confirmProposedDelete && onDeleteProposed != null) {
        AlertDialog(
            onDismissRequest = { confirmProposedDelete = false },
            title = { Text("일정을 삭제할까요?") },
            text = { Text("동행자들이 삭제에 동의했을 때 지워 주세요. '${original.displayTitle}' 일정이 사라져요.") },
            confirmButton = {
                TextButton(onClick = { confirmProposedDelete = false; onDeleteProposed() }) {
                    Text("삭제", color = TimelineColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmProposedDelete = false }) { Text("취소") } },
        )
    }

    if (proposingDelete && onProposeDelete != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { proposingDelete = false },
            title = { Text("삭제를 제안할까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "'${original.displayTitle}' 은(는) " +
                            (if (original.locked) "잠긴" else "확정된") +
                            " 일정이라 바로 지울 수 없어요. 동행자들에게 지울지 물어볼게요.",
                        fontSize = 14.sp,
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("이유 (예: 비 소식이 있어서)", fontSize = 13.sp) },
                        minLines = 2,
                        shape = FieldShape,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reason.isNotBlank(),
                    onClick = { proposingDelete = false; onProposeDelete(reason.trim()) },
                ) { Text("삭제 제안", color = if (reason.isNotBlank()) TimelineColors.Error else TimelineColors.Axis) }
            },
            dismissButton = { TextButton(onClick = { proposingDelete = false }) { Text("취소") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("블록을 삭제할까요?") },
            text = { Text("'${original.displayTitle}' 블록이 삭제돼요.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("삭제", color = TimelineColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

/**
 * 확정 블록의 제목이 비어 있거나 '예전 장소 이름 그대로'였다면, 새 장소 이름을 제목으로 쓴다.
 * (직접 적은 제목은 건드리지 않음)
 */
internal fun Block.withAutoTitle(original: Block): Block {
    if (kind != BlockKind.FIXED) return this
    val p = place?.trim().orEmpty()
    if (p.isEmpty()) return this
    val followsPlace = title.isBlank() || title == original.place
    return if (followsPlace) copy(title = p, place = p) else copy(place = p)
}

/* ───────── 머리: [자유 | 확정]  ✕ ───────── */

@Composable
private fun Header(draft: Block, editable: Boolean, onKind: (BlockKind) -> Unit, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (draft.kind == BlockKind.MOVE) {
            Text("이동 블록", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        } else {
            KindToggle(draft.kind, editable, onKind)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Text("✕", fontSize = 18.sp, color = TimelineColors.SubText) }
    }
}

@Composable
private fun KindToggle(kind: BlockKind, enabled: Boolean, onChange: (BlockKind) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .width(140.dp)
            .clip(shape)
            .border(1.dp, TimelineColors.Border, shape),
    ) {
        listOf(BlockKind.FREE to "자유", BlockKind.FIXED to "확정").forEach { (k, label) ->
            val selected = k == kind
            val color = if (k == BlockKind.FIXED) TimelineColors.Fixed else TimelineColors.Free
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) color else Color.White)
                    .clickable(enabled = enabled && !selected) { onChange(k) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun StatusHint(original: Block, draft: Block, isNew: Boolean, needsProposal: Boolean = false, editingProposal: Boolean = false) {
    val timeChanged = original.start != draft.start || original.end != draft.end
    val text = if (needsProposal) {
        if (editingProposal) "✏️ 수정 제안 중 · 고친 내용은 '제안됨'으로 바뀌고 다시 투표해요."
        else "확정된 일정은 바로 고칠 수 없어요. 아래 [삭제/수정 제안하기]로 동행자에게 물어보세요."
    } else when (draft.kind) {
        BlockKind.MOVE -> "시간·이동수단·메모를 바꿀 수 있어요."
        BlockKind.FREE -> "자유 블록은 메모만 남겨요. 비워두면 '빈칸'으로 보여요."
        BlockKind.FIXED -> when {
            isNew -> "새 확정 블록은 '제안됨'으로 추가되고, 동행자 투표로 확정돼요."
            original.kind != BlockKind.FIXED -> "확정 블록으로 바꾸면 '제안됨'이 돼요."
            timeChanged && original.status == BlockStatus.CONFIRMED -> "시간을 바꾸면 다시 '제안됨'이 돼요."
            else -> "현재 상태: " + when (draft.status) {
                BlockStatus.CONFIRMED -> "확정됨"
                BlockStatus.VOTING -> "투표중"
                BlockStatus.PROPOSED -> "제안됨"
            }
        }
    }
    Text(text, fontSize = 12.sp, color = TimelineColors.SubText)
}

/* ───────── 칸들 ───────── */

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedBorderColor = Black,
    unfocusedBorderColor = FieldBorder,
    disabledBorderColor = TimelineColors.Axis,
)

/** 장소: 가운데 정렬 글자 + 오른쪽 [검색] */
@Composable
private fun PlaceField(
    value: String,
    enabled: Boolean,
    searching: Boolean,
    onChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
        placeholder = { Text("예) 오이도 빨간등대", fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        trailingIcon = {
            // 돋보기 아이콘 (찾는 중엔 회색)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled && !searching, onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                SearchIcon(if (enabled && !searching) Black else TimelineColors.Axis)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = FieldShape,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
    )
}

/** 돋보기 아이콘 (아이콘 라이브러리 없이 직접 그림) */
@Composable
private fun SearchIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = 2.2.dp.toPx()
        val r = this.size.minDimension * 0.32f
        val c = Offset(this.size.width * 0.42f, this.size.height * 0.42f)
        drawCircle(color, radius = r, center = c, style = Stroke(width = stroke))
        val k = 0.7071f
        drawLine(
            color,
            start = Offset(c.x + r * k, c.y + r * k),
            end = Offset(this.size.width * 0.9f, this.size.height * 0.9f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** 모달 아래 큰 검정 버튼 */
@Composable
private fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) Black else Color(0xFFD6D6D6))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 16.sp) }
}

/** 제안 고르기 한 줄 */
@Composable
private fun ProposalOption(title: String, desc: String, danger: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (danger) Color(0xFFFDECEA) else Color(0xFFF1F7FC))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (danger) TimelineColors.Error else Black)
        Text(desc, fontSize = 12.sp, color = TimelineColors.SubText)
    }
}

/** 지도 위 작은 버튼 (전체 보기 · 닫기) */
@Composable
private fun MapCornerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Black,
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, FieldBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * 지도 전체 화면. 작은 지도와 같은 핀·이전 일정·시흥 테두리를 크게 보여준다.
 * 여기서 눌러 핀을 옮겨도 모달에 그대로 반영된다.
 */
@Composable
private fun FullMapDialog(
    draft: Block,
    prev: Block?,
    editable: Boolean,
    message: String?,
    onPick: (Double, Double) -> Unit,
    onOutside: () -> Unit,
    onChoose: (PlaceResult) -> Unit,
    onClose: () -> Unit,
    spots: List<MapSpot> = emptyList(),
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(draft.place.orEmpty()) }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    fun search() {
        val q = query.trim()
        focus.clearFocus()
        if (q.isEmpty()) {
            note = "장소 이름을 먼저 적어주세요."
            return
        }
        scope.launch {
            searching = true
            note = null
            results = vworldSearch(q)
            if (results.isEmpty()) {
                // 브이월드에서 못 찾으면 안드로이드 기본 검색으로 한 번 더
                val hit = searchPlace(context, q)
                if (hit != null) {
                    onChoose(PlaceResult(q, "", null, hit.lat, hit.lng))
                } else {
                    note = "시흥 안에서 '${q}'을(를) 찾지 못했어요."
                }
            }
            searching = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            PlaceMap(
                lat = draft.lat,
                lng = draft.lng,
                enabled = editable,
                onPick = onPick,
                onOutside = onOutside,
                prevLat = prev?.lat,
                prevLng = prev?.lng,
                spots = spots,
                modifier = Modifier.fillMaxSize(),
            )

            // 위: 왼쪽 검색창 · 오른쪽 닫기 · 그 아래 검색 결과
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; note = null },
                        enabled = editable,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        placeholder = { Text("가게·장소 검색", fontSize = 14.sp) },
                        trailingIcon = {
                            // 돋보기 아이콘 (찾는 중엔 회색)
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = editable && !searching) { search() },
                                contentAlignment = Alignment.Center,
                            ) {
                                SearchIcon(if (editable && !searching) Black else TimelineColors.Axis)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        shape = RoundedCornerShape(50),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    MapCornerButton("✕", onClick = onClose)
                }
                if (results.isNotEmpty()) {
                    SearchResults(
                        results,
                        onChoose = { r ->
                            onChoose(r)
                            query = r.name
                            results = emptyList()
                        },
                        onClose = { results = emptyList() },
                    )
                }
                note?.let {
                    Text(
                        it, fontSize = 12.sp, color = TimelineColors.Error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // 아래: 지금 고른 장소 카드
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    draft.place?.takeIf { it.isNotBlank() } ?: if (draft.hasLocation) "고른 위치" else "아직 위치를 안 골랐어요",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                )
                draft.address?.takeIf { it.isNotBlank() }?.let { AddressLine(it) }
                if (prev != null && draft.lat != null && draft.lng != null) {
                    val m = distanceMeters(prev.lat!!, prev.lng!!, draft.lat, draft.lng)
                    Text(
                        "이전 일정 ${prev.displayTitle}에서 직선 ${formatDistance(m)} · 도보 약 ${walkMinutes(m)}분",
                        fontSize = 12.sp, color = TimelineColors.SubText,
                    )
                }
                Text(
                    message ?: if (editable) "검색하거나 지도를 눌러 위치를 고르세요." else "잠긴 블록이라 위치를 바꿀 수 없어요.",
                    fontSize = 11.sp,
                    color = if (message != null) TimelineColors.Error else TimelineColors.Axis,
                )

                // 결정 → 모달로 돌아가기 (고른 장소는 이미 모달에 들어가 있음)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(if (draft.hasLocation) Black else Color(0xFFD6D6D6))
                        .clickable(enabled = draft.hasLocation, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Text("이 위치로 결정", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

/** 검색 결과 목록 — 누르면 그 장소로 */
@Composable
private fun SearchResults(results: List<PlaceResult>, onChoose: (PlaceResult) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, FieldBorder, FieldShape),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("검색 결과 ${results.size}개 · 눌러서 고르기", fontSize = 12.sp, color = TimelineColors.SubText, modifier = Modifier.weight(1f))
            Text(
                "닫기", fontSize = 12.sp, color = TimelineColors.SubText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(8.dp),
            )
        }
        results.forEachIndexed { i, r ->
            if (i > 0) HorizontalDivider(color = Color(0xFFEDEDED))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChoose(r) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    r.category?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(it.substringAfterLast('>').trim(), fontSize = 11.sp, color = TimelineColors.SubText)
                    }
                }
                if (r.address.isNotBlank() && r.address != r.name) {
                    Text(r.address, fontSize = 12.sp, color = TimelineColors.SubText)
                }
            }
        }
    }
}

/** 상세 주소 한 줄 */
@Composable
private fun AddressLine(address: String) {
    Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.Top) {
        Text("📍", fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(address, fontSize = 13.sp, color = Color(0xFF424242))
    }
}

/**
 * 이전 일정과의 거리 — 직선거리 + 대략 도보/차 시간 + 사이에 비는 시간.
 * 도보로 비는 시간 안에 못 갈 것 같으면 주황색으로 알려준다.
 */
@Composable
private fun PrevDistanceCard(prev: Block, lat: Double, lng: Double, start: LocalTime) {
    val m = distanceMeters(prev.lat!!, prev.lng!!, lat, lng)
    val gap = start.toMinutes() - prev.end.toMinutes()
    val walk = walkMinutes(m)
    val car = carMinutes(m)
    val tight = walk > gap
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (tight) Color(0xFFFFF3E0) else Color.White)
            .border(1.dp, if (tight) Color(0xFFFFB74D) else Color(0xFFDDDDDD), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            "이전 일정 · ${prev.displayTitle} (${prev.end.format(HM)} 끝)",
            fontSize = 12.sp, color = TimelineColors.SubText,
        )
        Text(
            "직선 ${formatDistance(m)} · 도보 약 ${walk}분 · 차 약 ${car}분",
            fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
        Text(
            if (gap <= 0) "이전 일정이 끝나자마자 시작해요." else "사이에 비는 시간 ${gap}분" + if (tight) " — 걸어가기엔 빠듯해요" else "",
            fontSize = 12.sp,
            color = if (tight) Color(0xFFE65100) else TimelineColors.SubText,
        )
        Text("※ 직선거리 기준 대략적인 시간이에요.", fontSize = 10.sp, color = TimelineColors.Axis)
    }
}

/** 시간: "15:30 ~ 16:30" — 누르면 휠 선택창 */
@Composable
private fun TimeField(draft: Block, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, if (enabled) FieldBorder else TimelineColors.Axis, FieldShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(draft.timeText(), fontSize = 16.sp, color = if (enabled) Color.Black else TimelineColors.SubText)
        Text(
            "${draft.durationMinutes}분",
            fontSize = 12.sp, color = TimelineColors.SubText,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun BoxField(
    value: String,
    enabled: Boolean,
    minLines: Int = 1,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = minLines == 1,
        minLines = minLines,
        textStyle = TextStyle(fontSize = 15.sp),
        placeholder = if (placeholder != null) { { Text(placeholder, fontSize = 14.sp) } } else null,
        shape = FieldShape,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
    )
}

/* ───────── 할 일 (마크다운: 편집 / 미리보기) ───────── */

@Composable
private fun TodoEditor(
    md: String,
    enabled: Boolean,
    /** 편집은 못 하고 체크만 할 수 있을 때 (잠긴 블록) */
    onToggleOnly: ((Int) -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    var preview by remember { mutableStateOf(md.isNotBlank()) }
    val (done, total) = todoProgress(md)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("할 일", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
            if (total > 0) Text("  $done/$total", fontSize = 12.sp, color = TimelineColors.SubText)
            Spacer(Modifier.weight(1f))
            SmallChip("편집", selected = !preview) { preview = false }
            Spacer(Modifier.width(4.dp))
            SmallChip("미리보기", selected = preview) { preview = true }
        }
        if (preview) {
            Box(
                Modifier.fillMaxWidth().clip(FieldShape).background(Color.White)
                    .border(1.dp, FieldBorder, FieldShape).padding(12.dp),
            ) {
                MarkdownView(
                    md,
                    enabled = enabled || onToggleOnly != null,
                    onToggle = { line -> if (onToggleOnly != null) onToggleOnly(line) else onChange(toggleCheck(md, line)) },
                )
            }
        } else {
            OutlinedTextField(
                value = md,
                onValueChange = onChange,
                enabled = enabled,
                minLines = 5,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                placeholder = { Text("## 준비물\n- [ ] 수영복\n- **10시까지** 입장", fontSize = 13.sp) },
                shape = FieldShape,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "- [ ] 체크리스트 · - 목록 · ## 제목 · **굵게**",
                fontSize = 11.sp, color = TimelineColors.SubText,
            )
        }
    }
}

/* ───────── 태그 · 이동수단 ───────── */

/** 와이어프레임: #맛집 #명소 — 고른 건 초록 */
@Composable
private fun TagPicker(tags: List<String>, enabled: Boolean, onChange: (List<String>) -> Unit) {
    // 5개를 한 줄에: 칸을 똑같이 나누고 글자를 조금 작게
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TAG_OPTIONS.forEach { t ->
            val on = t in tags
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (on) TagOn else Color.White)
                    .border(1.dp, if (on) TagOnBorder else FieldBorder, shape)
                    .clickable(enabled = enabled) { onChange(if (on) tags - t else tags + t) }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$t",
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = if (enabled) Color.Black else TimelineColors.SubText,
                )
            }
        }
    }
}

@Composable
private fun TransportPicker(selected: Transport?, enabled: Boolean, onChange: (Transport) -> Unit) {
    Section("이동수단") {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Transport.entries.forEach { t ->
                SmallChip(t.label, selected = t == selected, enabled = enabled) { onChange(t) }
            }
        }
    }
}

/* ───────── 공통 작은 부품 ───────── */

@Composable
private fun SmallChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        fontSize = 12.sp,
        color = when {
            !enabled -> TimelineColors.Axis
            selected -> Color.White
            else -> Color.Black
        },
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Black else Color.White)
            .border(1.dp, if (selected) Black else TimelineColors.Border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/* ───────── Preview ───────── */

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "일정 모달 · 확정(명소)")
@Composable
private fun BlockEditorPreview() {
    val block = Block(
        id = "b99", tripId = "preview", date = LocalDate.of(2026, 11, 7),
        start = LocalTime.of(15, 30), end = LocalTime.of(16, 30),
        kind = BlockKind.FIXED, status = BlockStatus.PROPOSED, source = BlockSource.USER,
        title = "집", place = "집", tags = listOf("명소"), memo = "밥 먹기",
        lat = 37.3800, lng = 126.7840,
    )
    BlockEditorSheet(
        original = block, isNew = false, allBlocks = listOf(block),
        onDismiss = {}, onSave = {}, onDelete = {}, onVoteClick = {},
    )
}
