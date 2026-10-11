package com.mbtrip.timeline

import com.mbtrip.timeline.fake.FakeBlocks
import com.mbtrip.timeline.model.BlockDisplay
import com.mbtrip.timeline.model.BlockSource
import com.mbtrip.timeline.model.BlockStatus
import com.mbtrip.timeline.ui.TimelineItem
import com.mbtrip.timeline.ui.applyEdit
import com.mbtrip.timeline.ui.buildItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FakeBlocksTest {

    @Test fun `같은 날 블록 시간이 겹치지 않는다`() {
        FakeBlocks.byDate.values.forEach { day ->
            day.zipWithNext().forEach { (a, b) -> assertTrue("${a.id}↔${b.id} 겹침", a.end <= b.start) }
        }
    }

    @Test fun `상태·출처·표시색이 모두 한 번 이상 등장한다`() {
        val bs = FakeBlocks.blocks
        assertEquals(BlockStatus.entries.toSet(), bs.map { it.status }.toSet())
        assertEquals(BlockSource.entries.toSet(), bs.map { it.source }.toSet())
        assertEquals(BlockDisplay.entries.toSet(), bs.map { it.display }.toSet())
    }

    @Test fun `id 가 중복되지 않는다`() {
        assertEquals(FakeBlocks.blocks.size, FakeBlocks.blocks.map { it.id }.toSet().size)
    }

    @Test fun `현재 시각 선은 시작 시각 기준 올바른 위치에 들어간다`() {
        val day1 = FakeBlocks.byDate.getValue(FakeBlocks.DAY1)
        val items = buildItems(day1, LocalTime.of(14, 10))
        val idx = items.indexOf(TimelineItem.NowItem)
        assertEquals("b05", (items[idx - 1] as TimelineItem.BlockItem).block.id)
        assertEquals("b06", (items[idx + 1] as TimelineItem.BlockItem).block.id)
        assertTrue(TimelineItem.NowItem !in buildItems(day1, LocalTime.of(6, 0)))
    }

    @Test fun `잘못된 시간 입력은 기존 시간을 유지하고 version 은 오른다`() {
        val b = FakeBlocks.blocks.first { it.id == "b07" }
        val bad = b.applyEdit(b.tags, "배곧", "17:00 ~ 16:00", "")
        assertEquals(b.start, bad.start); assertEquals(b.version + 1, bad.version)
        val ok = b.applyEdit(b.tags, "배곧", "16:10 ~ 17:00", "")
        assertEquals(LocalTime.of(16, 10), ok.start)
    }
}
