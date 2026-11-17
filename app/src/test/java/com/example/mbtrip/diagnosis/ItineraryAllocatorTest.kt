package com.example.mbtrip.diagnosis

import com.example.mbtrip.timeline.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class ItineraryAllocatorTest {
    private val date = LocalDate.of(2026, 11, 17)
    private fun block(id: String, start: Int, end: Int, kind: BlockKind = BlockKind.FIXED) = Block(
        id, "trip", date, LocalTime.of(start, 0), LocalTime.of(end, 0), kind,
        BlockStatus.CONFIRMED, BlockSource.USER, title = "필수 일정", locked = true,
        transport = if (kind == BlockKind.MOVE) Transport.TRAIN else null,
    )
    private fun allocate(
        required: List<Block> = emptyList(),
        candidates: List<PlannedActivity> = emptyList(),
        scores: List<Int> = listOf(20, 80),
    ) = ItineraryAllocator.allocateDay(
        "trip", date, LocalTime.of(9, 0), LocalTime.of(17, 0), scores, required, candidates,
    )
    private fun minutes(block: Block) = Duration.between(block.start, block.end).toMinutes().toInt()
    private fun assertFullDay(blocks: List<Block>) {
        assertEquals(LocalTime.of(9, 0), blocks.first().start)
        assertEquals(LocalTime.of(17, 0), blocks.last().end)
        blocks.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
        assertEquals(480, blocks.sumOf(::minutes))
        assertEquals(blocks.size, blocks.map { it.id }.toSet().size)
        assertTrue(blocks.all { it.tripId == "trip" && it.date == date })
    }

    @Test fun ratioThresholdsIncludeBothSidesOfEachBoundary() {
        for ((gap, fixed) in listOf(0 to 60, 15 to 60, 16 to 40, 35 to 40, 36 to 25, 100 to 25)) {
            assertEquals(AllocationRatio(fixed, 100 - fixed), ItineraryAllocator.ratioFor(listOf(0, gap)))
        }
    }
    @Test fun aSingleMemberKeepsTheExistingSixtyFortyRule() {
        assertEquals(AllocationRatio(60, 40), ItineraryAllocator.ratioFor(listOf(100)))
    }
    @Test fun emptyOrInvalidMemberScoresAreRejected() {
        for (scores in listOf(emptyList(), listOf(-1), listOf(101))) {
            assertThrows(IllegalArgumentException::class.java) { ItineraryAllocator.ratioFor(scores) }
        }
    }
    @Test fun percentagesMustBeWithinZeroAndOneHundred() {
        assertThrows(IllegalArgumentException::class.java) { AllocationRatio(-1, 101) }
    }
    @Test fun candidatesFillTheFixedTargetAndTheRestIsFree() {
        val required = block("lunch", 12, 13)
        val result = allocate(listOf(required), listOf(PlannedActivity("카페", 60)))
        assertTrue(result.contains(required))
        assertEquals(120, result.filter { it.kind == BlockKind.FIXED }.sumOf(::minutes))
        val proposed = result.single { it.source == BlockSource.AUTO && it.kind == BlockKind.FIXED }
        assertEquals(BlockStatus.PROPOSED, proposed.status)
        assertEquals(LocalTime.of(9, 0), proposed.start)
        assertFullDay(result)
    }
    @Test fun noCandidatesLeavesAllRemainingTimeFree() {
        val result = allocate(listOf(block("lunch", 12, 13)))
        assertEquals(420, result.filter { it.kind == BlockKind.FREE }.sumOf(::minutes))
        assertFullDay(result)
    }
    @Test fun requiredTimeMayExceedTargetWithoutBeingShortened() {
        val required = block("reservation", 10, 15)
        val result = allocate(listOf(required), listOf(PlannedActivity("추가", 30)))
        assertTrue(result.contains(required))
        assertEquals(1, result.count { it.kind == BlockKind.FIXED })
        assertFullDay(result)
    }
    @Test fun movementRemainsIntactAndDoesNotCountAsFixedActivity() {
        val move = block("train", 9, 10, BlockKind.MOVE)
        val result = allocate(listOf(move), listOf(PlannedActivity("식사", 120)))
        assertTrue(result.contains(move))
        assertEquals(120, result.filter { it.kind == BlockKind.FIXED }.sumOf(::minutes))
        assertFullDay(result)
    }
    @Test fun candidatesAreTriedInPriorityOrderAndOversizedOnesAreSkipped() {
        val result = allocate(candidates = listOf(
            PlannedActivity("너무 김", 121), PlannedActivity("첫 후보", 60),
            PlannedActivity("둘째 후보", 60), PlannedActivity("추가 후보", 30),
        ))
        assertEquals(listOf("첫 후보", "둘째 후보"), result.filter { it.kind == BlockKind.FIXED }.map { it.title })
        assertFullDay(result)
    }
    @Test fun candidateNeedsOneContinuousGap() {
        val required = listOf(block("a", 10, 11, BlockKind.MOVE), block("b", 12, 13, BlockKind.MOVE),
            block("c", 14, 15, BlockKind.MOVE), block("d", 16, 17, BlockKind.MOVE))
        val result = allocate(required, listOf(PlannedActivity("두 시간", 120)))
        assertTrue(result.none { it.kind == BlockKind.FIXED })
        assertFullDay(result)
    }
    @Test fun overlappingRequiredBlocksAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            allocate(listOf(block("a", 10, 12), block("b", 11, 13)))
        }
    }
    @Test fun requiredBlocksOutsideTheDayOrTripAreRejected() {
        for (required in listOf(block("x", 8, 10), block("x", 16, 18),
            block("x", 10, 11).copy(tripId = "other"), block("x", 10, 11).copy(date = date.plusDays(1)))) {
            assertThrows(IllegalArgumentException::class.java) { allocate(listOf(required)) }
        }
    }
    @Test fun generatedIdsCannotCollideWithRequiredBlockIds() {
        val required = listOf(block("auto_${date}_0", 12, 13), block("free_${date}_0", 14, 15, BlockKind.MOVE))
        val result = allocate(required, listOf(PlannedActivity("카페", 60)))
        assertTrue(result.containsAll(required))
        assertFullDay(result)
    }
    @Test fun duplicateRequiredIdsAreRejectedEvenWhenTimesDiffer() {
        assertThrows(IllegalArgumentException::class.java) {
            allocate(listOf(block("same", 10, 11), block("same", 12, 13)))
        }
    }
    @Test fun blankTripAndInvalidDayWindowAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ItineraryAllocator.allocateDay(" ", date, LocalTime.of(9, 0), LocalTime.of(17, 0), listOf(50), emptyList(), emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItineraryAllocator.allocateDay("trip", date, LocalTime.of(17, 0), LocalTime.of(9, 0), listOf(50), emptyList(), emptyList())
        }
    }
    @Test fun completeReservationDayCreatesNoExtraBlocks() {
        val required = block("all", 9, 17)
        assertEquals(listOf(required), allocate(listOf(required)))
    }
    @Test fun allocationIsDeterministicForIdenticalInputs() {
        val candidates = listOf(PlannedActivity("카페", 60))
        assertEquals(allocate(candidates = candidates), allocate(candidates = candidates))
    }
}
