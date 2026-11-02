package com.example.mbtrip.timeline

import com.example.mbtrip.timeline.data.BlockFields
import com.example.mbtrip.timeline.data.FakeBlockRepository
import com.example.mbtrip.timeline.data.blockFromFirestore
import com.example.mbtrip.timeline.data.toFirestoreMap
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.vote.VoteChoice
import com.example.mbtrip.timeline.vote.canSubmitVote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockDataTest {

    private val trip = FakeBlocks.TRIP_ID

    @Test fun 모든_가짜_블록이_Firestore_변환_후_그대로_돌아온다() {
        FakeBlocks.blocks.forEach { b ->
            val back = blockFromFirestore(b.id, b.tripId, b.toFirestoreMap())
            assertEquals(b, back)
        }
    }

    @Test fun 날짜와_시간은_글자로_저장된다() {
        val m = FakeBlocks.blocks.first { it.id == "b01" }.toFirestoreMap()
        assertEquals("2026-11-07", m[BlockFields.DATE])
        assertEquals("08:30", m[BlockFields.START])
        assertEquals("TRAIN", m[BlockFields.TRANSPORT])
    }

    @Test fun Firestore_숫자는_Long_으로_와도_읽힌다() {
        val m = FakeBlocks.blocks.first { it.id == "b03" }.toFirestoreMap().toMutableMap()
        m[BlockFields.VERSION] = 3L
        assertEquals(3, blockFromFirestore("b03", trip, m)!!.version)
    }

    @Test fun 형식이_틀린_문서는_건너뛴다() {
        assertNull(blockFromFirestore("x", trip, mapOf(BlockFields.DATE to "어제")))
        val badRule = FakeBlocks.blocks.first { it.id == "b05" }.toFirestoreMap().toMutableMap()
        badRule[BlockFields.STATUS] = "PROPOSED" // 자유 블록은 제안됨 불가 → 규칙 위반 문서
        assertNull(blockFromFirestore("b05", trip, badRule))
    }

    @Test fun 가짜_저장소_추가_수정_삭제() = runBlocking {
        val repo = FakeBlockRepository(FakeBlocks.blocks)
        val b = FakeBlocks.blocks.first { it.id == "b07" }
        repo.upsert(b.copy(memo = "바다 보기"))
        assertEquals("바다 보기", repo.observeBlocks(trip).first().first { it.id == "b07" }.memo)
        repo.delete(trip, "b07")
        assertTrue(repo.observeBlocks(trip).first().none { it.id == "b07" })
        assertEquals("b17", repo.newId(trip))
    }

    @Test fun 반대는_사유가_있어야_보낼_수_있다() {
        assertTrue(!canSubmitVote(null, "아무거나"))
        assertTrue(canSubmitVote(VoteChoice.AGREE, ""))
        assertTrue(!canSubmitVote(VoteChoice.DISAGREE, "   "))
        assertTrue(canSubmitVote(VoteChoice.DISAGREE, "피곤할 듯"))
    }
}
