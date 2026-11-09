package com.example.mbtrip.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.model.Transport
import java.time.LocalTime

private val TAG_OPTIONS = listOf("맛집", "명소", "카페")

/**
 * 블록 추가·수정 모달 (블록 한 번 누르기 / + 버튼).
 *  - 확정: 제목, 시간, 장소, 할 일(마크다운), 메모, 태그
 *  - 자유: 시간, 메모만
 *  - 이동: 시간, 이동수단, 메모
 *  - 맨 위 [자유 | 확정] 버튼으로 서로 전환 (이동은 전환 불가)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockEditorSheet(
    original: Block,
    isNew: Boolean,
    allBlocks: List<Block>,
    onDismiss: () -> Unit,
    onSave: (Block) -> Unit,
    onDelete: () -> Unit,
    onVoteClick: () -> Unit,
) {
    var draft by remember(original.id, isNew) { mutableStateOf(original) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editable = !original.locked
    val problem = remember(draft, allBlocks) { validate(allBlocks, draft) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1) 종류
            if (draft.kind == BlockKind.MOVE) {
                Text("이동 블록", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                KindToggle(draft.kind, editable) { draft = draft.withKind(it) }
            }
            StatusHint(original, draft, isNew)

            // 2) 시간
            TimeEditor(draft, editable) { draft = it }

            // 3) 종류별 항목
            when (draft.kind) {
                BlockKind.FIXED -> {
                    LabeledField("제목", draft.title, editable, placeholder = "예) 웨이브파크 서핑") {
                        draft = draft.copy(title = it)
                    }
                    LabeledField("장소", draft.place.orEmpty(), editable, placeholder = "예) 시흥 웨이브파크") {
                        draft = draft.copy(place = it)
                    }
                    TodoEditor(draft.todo, editable) { draft = draft.copy(todo = it) }
                    TagPicker(draft.tags, editable) { draft = draft.copy(tags = it) }
                    LabeledField("메모", draft.memo.orEmpty(), editable, minLines = 2) { draft = draft.copy(memo = it) }
                }
                BlockKind.FREE -> {
                    LabeledField(
                        "메모", draft.memo.orEmpty(), editable, minLines = 4,
                        placeholder = "비워두면 '빈칸'으로 보여요",
                    ) { draft = draft.copy(memo = it) }
                }
                BlockKind.MOVE -> {
                    TransportPicker(draft.transport, editable) { draft = draft.copy(transport = it) }
                    LabeledField("메모", draft.memo.orEmpty(), editable, minLines = 2, placeholder = "예) 서울역 3번 출구") {
                        draft = draft.copy(memo = it)
                    }
                }
            }

            // 4) 안내 · 오류
            if (original.locked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LockIcon(TimelineColors.SubText, 12.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("잠긴 블록은 수정·이동·삭제할 수 없어요.", fontSize = 13.sp, color = TimelineColors.SubText)
                }
            }
            problem?.let { Text(it, fontSize = 13.sp, color = TimelineColors.Error) }

            // 5) 버튼
            if (!isNew && original.kind == BlockKind.FIXED && original.status != BlockStatus.CONFIRMED) {
                OutlinedButton(onClick = onVoteClick, modifier = Modifier.fillMaxWidth()) { Text("투표하기") }
            }
            Button(
                onClick = { onSave(draft) },
                enabled = editable && problem == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
            ) { Text(if (isNew) "추가하기" else "저장하기") }
            if (!isNew && editable) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("블록 삭제", color = TimelineColors.Error)
                }
            }
        }
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

/* ───────── 종류 전환 [자유 | 확정] ───────── */

@Composable
private fun KindToggle(kind: BlockKind, enabled: Boolean, onChange: (BlockKind) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
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
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun StatusHint(original: Block, draft: Block, isNew: Boolean) {
    val timeChanged = original.start != draft.start || original.end != draft.end
    val text = when (draft.kind) {
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
    Text(text, fontSize = 13.sp, color = TimelineColors.SubText)
}

/* ───────── 시간 (±버튼) ───────── */

@Composable
private fun TimeEditor(draft: Block, enabled: Boolean, onChange: (Block) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("시간", fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("${draft.durationMinutes}분", fontSize = 12.sp, color = TimelineColors.SubText)
        }
        TimeRow("시작", draft.start, enabled) { onChange(draft.shiftStart(it)) }
        TimeRow("종료", draft.end, enabled) { onChange(draft.shiftEnd(it)) }
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, enabled: Boolean, onShift: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = TimelineColors.SubText, modifier = Modifier.width(36.dp))
        Text(time.format(HM), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(-60 to "−1h", -10 to "−10", 10 to "+10", 60 to "+1h").forEach { (delta, text) ->
                SmallChip(text, selected = false, enabled = enabled) { onShift(delta) }
            }
        }
    }
}

/* ───────── 할 일 (마크다운: 편집 / 미리보기) ───────── */

@Composable
private fun TodoEditor(md: String, enabled: Boolean, onChange: (String) -> Unit) {
    var preview by remember { mutableStateOf(md.isNotBlank()) }
    val (done, total) = todoProgress(md)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("할 일", fontSize = 13.sp)
            if (total > 0) Text("  $done/$total", fontSize = 12.sp, color = TimelineColors.SubText)
            Spacer(Modifier.weight(1f))
            SmallChip("편집", selected = !preview) { preview = false }
            Spacer(Modifier.width(4.dp))
            SmallChip("미리보기", selected = preview) { preview = true }
        }
        if (preview) {
            MarkdownView(md, enabled = enabled, onToggle = { line -> onChange(toggleCheck(md, line)) })
        } else {
            OutlinedTextField(
                value = md,
                onValueChange = onChange,
                enabled = enabled,
                minLines = 5,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                placeholder = { Text("## 준비물\n- [ ] 수영복\n- **10시까지** 입장", fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
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

@Composable
private fun TagPicker(tags: List<String>, enabled: Boolean, onChange: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("태그", fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TAG_OPTIONS.forEach { t ->
                val on = t in tags
                SmallChip("#$t", selected = on, enabled = enabled) { onChange(if (on) tags - t else tags + t) }
            }
        }
    }
}

@Composable
private fun TransportPicker(selected: Transport?, enabled: Boolean, onChange: (Transport) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("이동수단", fontSize = 13.sp)
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
            .background(if (selected) Color(0xFF1E1E1E) else Color.White)
            .border(1.dp, if (selected) Color(0xFF1E1E1E) else TimelineColors.Border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    enabled: Boolean,
    minLines: Int = 1,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            singleLine = minLines == 1,
            minLines = minLines,
            placeholder = if (placeholder != null) { { Text(placeholder, fontSize = 14.sp) } } else null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}
