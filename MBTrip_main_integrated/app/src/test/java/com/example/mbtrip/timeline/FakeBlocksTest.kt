package com.example.mbtrip.timeline

import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.model.BlockDisplay
import com.example.mbtrip.timeline.model.BlockKind
import com.example.mbtrip.timeline.model.BlockSource
import com.example.mbtrip.timeline.model.BlockStatus
import com.example.mbtrip.timeline.ui.EditResult
import com.example.mbtrip.timeline.ui.MdLine
import com.example.mbtrip.timeline.ui.addBlock
import com.example.mbtrip.timeline.ui.dayWindowHours
import com.example.mbtrip.timeline.ui.durationMinutes
import com.example.mbtrip.timeline.ui.shiftEnd
import com.example.mbtrip.timeline.ui.deleteBlock
import com.example.mbtrip.timeline.ui.moveBlock
import com.example.mbtrip.timeline.ui.newDraft
import com.example.mbtrip.timeline.ui.parseMarkdown
import com.example.mbtrip.timeline.ui.splitBold
import com.example.mbtrip.timeline.ui.toMinutes
import com.example.mbtrip.timeline.ui.todoProgress
import com.example.mbtrip.timeline.ui.toggleCheck
import com.example.mbtrip.timeline.ui.tripDates
import com.example.mbtrip.timeline.ui.updateBlock
import com.example.mbtrip.timeline.ui.withKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FakeBlocksTest {

    private val all = FakeBlocks.blocks
    private fun b(id: String) = all.first { it.id == id }
    private fun ok(r: EditResult): List<Block> {
        assertTrue("실패: $r", r is EditResult.Ok)
        return (r as EditResult.Ok).blocks
    }

    /* ── 가짜 데이터 ── */

    @Test fun 같은_날_블록_시간이_겹치지_않는다() {
        all.groupBy { it.date }.values.forEach { day ->
            day.sortedBy { it.start }.zipWithNext().forEach { (a, c) -> assertTrue("${a.id}↔${c.id}", a.end <= c.start) }
        }
    }

    @Test fun 상태_출처_표시가_모두_등장한다() {
        assertEquals(BlockStatus.entries.toSet(), all.map { it.status }.toSet())
        assertEquals(BlockSource.entries.toSet(), all.map { it.source }.toSet())
        assertEquals(BlockDisplay.entries.toSet(), all.map { it.display }.toSet())
    }

    @Test fun id_가_중복되지_않는다() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun 이동과_자유는_항상_확정됨이다() {
        assertTrue(all.filter { it.kind != BlockKind.FIXED }.all { it.status == BlockStatus.CONFIRMED })
        val thrown = runCatching { b("b05").copy(status = BlockStatus.PROPOSED) }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)
    }

    @Test fun 이동_블록_제목은_이동수단으로_만든다() {
        assertEquals("기차로 이동", b("b01").displayTitle)
        assertEquals("택시로 이동", b("b10").displayTitle)
    }

    /* ── 추가 ── */

    @Test fun 새_확정_블록은_제안됨으로_빈_시간에_들어간다() {
        val draft = newDraft(all, FakeBlocks.TRIP_ID, FakeBlocks.DAY1, BlockKind.FIXED)!!
        assertEquals(BlockStatus.PROPOSED, draft.status)
        assertEquals("b17", draft.id)
        val after = ok(addBlock(all, draft))
        assertEquals(all.size + 1, after.size)
    }

    @Test fun 새_자유_블록은_확정됨이고_빈칸이다() {
        val draft = newDraft(all, FakeBlocks.TRIP_ID, FakeBlocks.DAY2, BlockKind.FREE)!!
        assertEquals(BlockStatus.CONFIRMED, draft.status)
        assertEquals(BlockDisplay.FREE_EMPTY, draft.display)
    }

    @Test fun 겹치는_블록은_추가되지_않는다() {
        val draft = newDraft(all, FakeBlocks.TRIP_ID, FakeBlocks.DAY1, BlockKind.FREE)!!
            .copy(start = LocalTime.of(11, 0), end = LocalTime.of(12, 0))
        assertTrue(addBlock(all, draft) is EditResult.Error)
    }

    /* ── 수정 · 종류 전환 · 삭제 ── */

    @Test fun 확정_블록의_시간을_바꾸면_제안됨이_된다() {
        val original = b("b15")
        val after = ok(updateBlock(all, original, original.copy(end = LocalTime.of(13, 30))))
        val edited = after.first { it.id == "b15" }
        assertEquals(BlockStatus.PROPOSED, edited.status)
        assertEquals(original.version + 1, edited.version)
    }

    @Test fun 확정_블록의_메모만_바꾸면_상태는_그대로다() {
        val original = b("b15")
        val after = ok(updateBlock(all, original, original.copy(memo = "창가 자리")))
        assertEquals(BlockStatus.CONFIRMED, after.first { it.id == "b15" }.status)
    }

    @Test fun 자유와_확정은_서로_바꿀_수_있고_내용은_남는다() {
        val fixed = b("b05").withKind(BlockKind.FIXED)
        assertEquals(BlockStatus.PROPOSED, fixed.status)
        val backToFree = b("b09").withKind(BlockKind.FREE)
        assertEquals(BlockStatus.CONFIRMED, backToFree.status)
        assertEquals("저녁 후보", backToFree.title) // 숨겨질 뿐 지워지지 않음
        assertEquals(BlockKind.MOVE, b("b01").withKind(BlockKind.FREE).kind) // 이동은 전환 불가
    }

    @Test fun 잠긴_블록은_수정_삭제_이동이_안_된다() {
        assertTrue(deleteBlock(all, "b03") is EditResult.Error)
        assertTrue(moveBlock(all, "b03", 13 * 60) is EditResult.Error)
        assertTrue(updateBlock(all, b("b03"), b("b03").copy(memo = "x")) is EditResult.Error)
        assertEquals(all.size - 1, ok(deleteBlock(all, "b05")).size)
    }

    /* ── 끌어서 이동 ── */

    @Test fun 끌어서_옮기면_10분_단위로_맞춰지고_길이는_그대로다() {
        // b07 자유 16:00~17:00 → 빈 곳(Day1 21:04)으로 끌면 21:00 에 맞춰진다
        val after = ok(moveBlock(all, "b07", 21 * 60 + 4))
        val moved = after.first { it.id == "b07" }
        assertEquals(LocalTime.of(21, 0), moved.start)
        assertEquals(LocalTime.of(22, 0), moved.end)
        assertEquals(BlockStatus.CONFIRMED, moved.status) // 자유는 그대로 확정됨
    }

    @Test fun 확정_블록을_옮기면_제안됨이_된다() {
        // b15 점심(확정됨) 12:10~13:20 → 15:30 으로
        val moved = ok(moveBlock(all, "b15", 15 * 60 + 30)).first { it.id == "b15" }
        assertEquals(BlockStatus.PROPOSED, moved.status)
        assertEquals(LocalTime.of(16, 40), moved.end)
    }

    @Test fun 겹치는_곳으로는_옮길_수_없다() {
        assertTrue(moveBlock(all, "b07", 14 * 60) is EditResult.Error)
    }

    @Test fun 보이는_범위_밖으로는_못_나간다() {
        // b14(80분)를 23시로 끌어도 22시 안에 끝나도록 20:40 시작으로 멈춘다
        val moved = ok(moveBlock(all, "b14", 23 * 60, window = 8..22)).first { it.id == "b14" }
        assertEquals(20 * 60 + 40, moved.start.toMinutes())
    }

    /* ── 날짜 · 시간축 ── */

    @Test fun 여행_날짜는_블록이_없는_날도_포함한다() {
        val dates = tripDates(FakeBlocks.TRIP_START, FakeBlocks.TRIP_START.plusDays(2))
        assertEquals(3, dates.size)
    }

    @Test fun 시간축은_기본_8시부터_22시이고_필요하면_넓어진다() {
        assertEquals(8..22, dayWindowHours(emptyList()))
        assertEquals(7..22, dayWindowHours(all.filter { it.date == FakeBlocks.DAY1 })) // 08:30 시작 → 7시부터
    }

    @Test fun 체크인_체크아웃은_블록이_아니라_선으로_표시한다() {
        assertTrue(all.none { it.title.contains("체크인") || it.title.contains("체크아웃") })
        assertEquals(listOf("체크인", "체크아웃"), FakeBlocks.markers.map { it.label })
        // 22시 체크인처럼 늦은 선이 있으면 시간축이 넓어진다
        assertEquals(8..23, dayWindowHours(emptyList(), listOf(22 * 60)))
    }

    @Test fun 블록은_최소_20분이다() {
        val b15 = b("b15")
        assertTrue(addBlock(all, b15.copy(id = "x", start = LocalTime.of(21, 0), end = LocalTime.of(21, 10))) is EditResult.Error)
        assertEquals(20, b15.shiftEnd(-120).durationMinutes)
        assertTrue(all.all { it.durationMinutes >= 20 })
    }

    /* ── 마크다운 ── */

    @Test fun 마크다운을_줄_종류별로_나눈다() {
        val lines = parseMarkdown(b("b03").todo)
        assertTrue(lines[0] is MdLine.Heading)
        assertEquals(MdLine.Check(1, true, "수영복"), lines[1])
        assertEquals(MdLine.Check(2, false, "선크림"), lines[2])
        assertTrue(lines[4] is MdLine.Bullet)
    }

    @Test fun 체크박스를_누르면_원문이_바뀐다() {
        val md = b("b03").todo
        val toggled = toggleCheck(md, 2)
        assertTrue(toggled.lines()[2].contains("[x]"))
        assertEquals(md, toggleCheck(toggled, 2))
        assertEquals(2 to 3, todoProgress(toggled))
    }

    @Test fun 굵게_표시를_나눈다() {
        assertEquals(listOf("10:20까지" to true, " 입장" to false), splitBold("**10:20까지** 입장"))
        assertNull(parseMarkdown("").filterIsInstance<MdLine.Check>().firstOrNull())
    }
}
