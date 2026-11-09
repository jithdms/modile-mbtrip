package com.example.mbtrip.timeline.ui

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/*
 * Compose 에 의존하지 않는 순수 로직 — JUnit 으로 바로 테스트 가능.
 * 2주차에 ViewModel 로 옮길 때 그대로 가져간다.
 */

internal val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val MD: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

internal fun Block.timeText() = "${start.format(HM)} ~ ${end.format(HM)}"

/* ───────── 시간 계산 ───────── */

/** 시간 이동 단위 (분) */
internal const val SNAP_MINUTES = 10

/** 블록 최소 길이 (분) — 이보다 짧으면 카드에 글자가 들어가지 않는다 (20분 = 32dp) */
internal const val MIN_BLOCK_MINUTES = 20

/** 하루 안에서 블록이 끝날 수 있는 가장 늦은 시각 (분). LocalTime 은 24:00 을 표현할 수 없음 */
internal const val DAY_LAST_MINUTE = 23 * 60 + 50

internal fun LocalTime.toMinutes(): Int = hour * 60 + minute
internal fun minutesToTime(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)

/** 10분 단위로 반올림 */
internal fun snapMinutes(m: Int): Int = (m / SNAP_MINUTES.toDouble()).roundToInt() * SNAP_MINUTES

internal val Block.durationMinutes: Int get() = end.toMinutes() - start.toMinutes()

/** 여행 시작~종료일의 날짜 목록 (블록이 없는 날 포함) */
internal fun tripDates(start: LocalDate, end: LocalDate): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()

/**
 * 시간축에 보여줄 범위 (시) — 기본 08~22시, 블록·체크인/아웃 선이 더 이르거나 늦으면 1시간 여유를 두고 넓힌다
 * @param extraMinutes 블록 외에 꼭 보여야 하는 시각 (체크인·체크아웃 등, 분)
 */
internal fun dayWindowHours(dayBlocks: List<Block>, extraMinutes: List<Int> = emptyList()): IntRange {
    val starts = dayBlocks.map { it.start.toMinutes() } + extraMinutes
    val ends = dayBlocks.map { it.end.toMinutes() } + extraMinutes
    if (starts.isEmpty()) return 8..22
    val first = starts.min()
    val last = ends.max()
    val startHour = minOf(8, first / 60 - 1).coerceAtLeast(0)
    val endHour = maxOf(22, (last + 59) / 60 + 1).coerceAtMost(24)
    return startHour..endHour
}

/* ───────── 겹침 검사 ───────── */

/** 같은 날 다른 블록과 시간이 겹치면 그 블록을 돌려준다 (경계가 맞닿는 건 허용) */
internal fun findConflict(blocks: List<Block>, candidate: Block): Block? =
    blocks.firstOrNull {
        it.id != candidate.id && it.date == candidate.date &&
            candidate.start < it.end && it.start < candidate.end
    }

/** 저장 전 검사. 문제가 없으면 null, 있으면 화면에 보여줄 문구 */
internal fun validate(blocks: List<Block>, candidate: Block): String? {
    if (candidate.durationMinutes < MIN_BLOCK_MINUTES) return "블록은 최소 ${MIN_BLOCK_MINUTES}분 이상이어야 해요."
    findConflict(blocks, candidate)?.let {
        return "${it.timeText()} '${it.displayTitle}' 블록과 시간이 겹쳐요."
    }
    return null
}

/* ───────── 추가 · 수정 · 삭제 · 이동 ───────── */

sealed interface EditResult {
    data class Ok(val blocks: List<Block>) : EditResult
    data class Error(val message: String) : EditResult
}

/** 상태 규칙: 이동·자유는 항상 확정됨 */
private fun Block.normalized(): Block =
    if (kind != BlockKind.FIXED && status != BlockStatus.CONFIRMED) copy(status = BlockStatus.CONFIRMED) else this

/** 새 블록 id — b01, b02 … 다음 번호 */
internal fun newBlockId(blocks: List<Block>): String {
    val max = blocks.mapNotNull { it.id.removePrefix("b").toIntOrNull() }.maxOrNull() ?: 0
    return "b" + (max + 1).toString().padStart(2, '0')
}

/** + 버튼으로 새 블록 초안 만들기. 확정 블록은 '제안됨'으로 시작. 빈 시간이 없으면 null */
internal fun newDraft(
    blocks: List<Block>, tripId: String, date: LocalDate, kind: BlockKind, lengthMinutes: Int = 60,
): Block? {
    val slot = nextFreeSlot(blocks, date, lengthMinutes) ?: return null
    return Block(
        id = newBlockId(blocks), tripId = tripId, date = date,
        start = slot, end = minutesToTime(slot.toMinutes() + lengthMinutes),
        kind = kind,
        status = if (kind == BlockKind.FIXED) BlockStatus.PROPOSED else BlockStatus.CONFIRMED,
        source = BlockSource.USER,
    )
}

/** 그날 비어 있는 첫 시간대 (08시부터 찾고, 없으면 0시부터) */
internal fun nextFreeSlot(blocks: List<Block>, date: LocalDate, lengthMinutes: Int): LocalTime? {
    val dayBlocks = blocks.filter { it.date == date }
    val candidates = (8 * 60..DAY_LAST_MINUTE - lengthMinutes step SNAP_MINUTES) +
        (0 until 8 * 60 step SNAP_MINUTES)
    return candidates.firstOrNull { s ->
        val e = s + lengthMinutes
        dayBlocks.none { s < it.end.toMinutes() && it.start.toMinutes() < e }
    }?.let(::minutesToTime)
}

internal fun addBlock(blocks: List<Block>, draft: Block): EditResult {
    val b = draft.normalized()
    validate(blocks, b)?.let { return EditResult.Error(it) }
    return EditResult.Ok(blocks + b)
}

/**
 * 모달에서 저장. 확정 블록의 시간이 바뀌거나 자유→확정으로 바뀌면 다시 '제안됨'.
 * version + 1.
 */
internal fun updateBlock(blocks: List<Block>, original: Block, edited: Block): EditResult {
    if (original.locked) return EditResult.Error("잠긴 블록은 수정할 수 없어요.")
    val timeChanged = original.start != edited.start || original.end != edited.end
    val becameFixed = original.kind != BlockKind.FIXED && edited.kind == BlockKind.FIXED
    val status = when {
        edited.kind != BlockKind.FIXED -> BlockStatus.CONFIRMED
        timeChanged || becameFixed -> BlockStatus.PROPOSED
        else -> edited.status
    }
    val b = edited.copy(status = status, version = original.version + 1)
    validate(blocks, b)?.let { return EditResult.Error(it) }
    return EditResult.Ok(blocks.map { if (it.id == b.id) b else it })
}

/**
 * 할 일 체크만 바꾸기 — 잠긴·확정 블록도 체크는 할 수 있다 (일정 자체는 안 바뀜).
 * 상태(확정·제안됨)는 그대로 둔다.
 */
internal fun toggleTodoInBlock(blocks: List<Block>, id: String, lineIndex: Int): EditResult {
    val b = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    val updated = b.copy(todo = toggleCheck(b.todo, lineIndex), version = b.version + 1)
    return EditResult.Ok(blocks.map { if (it.id == id) updated else it })
}

/**
 * 삭제 제안 — 확정·잠긴 블록은 바로 지우지 않고 '삭제 제안' 표시만 단다.
 * 실제 삭제 여부는 투표 로직(VoteActions.proposeDelete)이 정한다.
 */
internal fun requestDelete(blocks: List<Block>, id: String): EditResult {
    val b = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    if (b.deleteRequested) return EditResult.Error("이미 삭제 제안된 블록이에요.")
    val updated = b.copy(deleteRequested = true, version = b.version + 1)
    return EditResult.Ok(blocks.map { if (it.id == id) updated else it })
}

/**
 * 수정 제안 — 확정·잠긴 블록을 고친 내용으로 바꾸고 '제안됨'으로 되돌려 다시 투표에 올린다.
 * (잠금도 풀린다. 자유·이동 블록은 원래 규칙대로 확정 상태 유지)
 */
internal fun proposeEdit(blocks: List<Block>, original: Block, edited: Block): EditResult {
    validate(blocks, edited)?.let { return EditResult.Error(it) }
    val updated = edited.copy(
        status = if (edited.kind == BlockKind.FIXED) BlockStatus.PROPOSED else BlockStatus.CONFIRMED,
        locked = false,
        deleteRequested = false,
        version = original.version + 1,
    )
    return EditResult.Ok(blocks.map { if (it.id == original.id) updated else it })
}

/** 삭제 제안 취소 */
internal fun cancelDeleteRequest(blocks: List<Block>, id: String): EditResult {
    val b = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    val updated = b.copy(deleteRequested = false, version = b.version + 1)
    return EditResult.Ok(blocks.map { if (it.id == id) updated else it })
}

/** 삭제 제안이 받아들여진 뒤 실제로 지우기 (잠긴 블록도 지울 수 있음) */
internal fun deleteProposed(blocks: List<Block>, id: String): EditResult {
    val b = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    if (!b.deleteRequested) return EditResult.Error("먼저 삭제 제안을 해야 해요.")
    return EditResult.Ok(blocks.filterNot { it.id == id })
}

/** 확정됐거나 잠긴 블록은 바로 삭제 대신 '삭제 제안' 을 한다 */
internal fun Block.needsDeleteProposal(): Boolean = locked || (kind == BlockKind.FIXED && status == BlockStatus.CONFIRMED)

internal fun deleteBlock(blocks: List<Block>, id: String): EditResult {
    val target = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    if (target.locked) return EditResult.Error("잠긴 블록은 삭제할 수 없어요.")
    return EditResult.Ok(blocks.filterNot { it.id == id })
}

/**
 * 손가락으로 끌어서 시간 이동. 길이는 그대로, 10분 단위로 맞춘다.
 * 확정 블록이 실제로 움직이면 '제안됨'으로 바뀐다.
 * @param window 시간축에 보이는 범위(시). 이 안으로만 움직일 수 있다.
 */
internal fun moveBlock(
    blocks: List<Block>, id: String, requestedStartMinutes: Int, window: IntRange = 0..24,
): EditResult {
    val b = blocks.firstOrNull { it.id == id } ?: return EditResult.Error("블록을 찾을 수 없어요.")
    if (b.locked) return EditResult.Error("잠긴 블록은 옮길 수 없어요.")
    val newStart = clampStart(snapMinutes(requestedStartMinutes), b.durationMinutes, window)
    if (newStart == b.start.toMinutes()) return EditResult.Ok(blocks)
    val moved = b.copy(
        start = minutesToTime(newStart),
        end = minutesToTime(newStart + b.durationMinutes),
        status = if (b.kind == BlockKind.FIXED) BlockStatus.PROPOSED else b.status,
        version = b.version + 1,
    )
    findConflict(blocks, moved)?.let {
        return EditResult.Error("'${it.displayTitle}' 블록과 겹쳐서 옮길 수 없어요.")
    }
    return EditResult.Ok(blocks.map { if (it.id == id) moved else it })
}

/** 시작 시각을 보이는 범위 안으로 제한 */
internal fun clampStart(startMinutes: Int, durationMinutes: Int, window: IntRange): Int {
    val min = window.first * 60
    val max = minOf(window.last * 60, DAY_LAST_MINUTE) - durationMinutes
    return startMinutes.coerceIn(min, maxOf(min, max))
}

/** 모달의 [자유 | 확정] 전환. 자유→확정은 '제안됨', 확정→자유는 '확정됨'. 제목·장소·할 일은 지우지 않는다 */
internal fun Block.withKind(kind: BlockKind): Block = when {
    this.kind == BlockKind.MOVE || kind == BlockKind.MOVE || kind == this.kind -> this
    kind == BlockKind.FIXED -> copy(kind = kind, status = BlockStatus.PROPOSED)
    else -> copy(kind = kind, status = BlockStatus.CONFIRMED)
}

/** 모달의 ±버튼 — 시작/종료를 delta 분만큼. 시작 < 종료, 하루 범위 유지 */
internal fun Block.shiftStart(delta: Int): Block {
    val s = (start.toMinutes() + delta).coerceIn(0, end.toMinutes() - MIN_BLOCK_MINUTES)
    return copy(start = minutesToTime(s))
}

internal fun Block.shiftEnd(delta: Int): Block {
    val e = (end.toMinutes() + delta).coerceIn(start.toMinutes() + MIN_BLOCK_MINUTES, DAY_LAST_MINUTE)
    return copy(end = minutesToTime(e))
}
