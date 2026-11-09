package com.example.mbtrip.timeline.data

import com.example.mbtrip.timeline.model.Block
import com.example.mbtrip.timeline.ui.newBlockId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 일정 블록 저장소 — 화면(ViewModel)은 이 약속만 보고, 진짜 저장 위치는 몰라도 된다.
 *  - FakeBlockRepository     : 메모리 (미리보기·테스트·Firebase 설정 전)
 *  - FirestoreBlockRepository: Firebase (설정 후 TimelineDependencies 에서 바꿔 끼움)
 */
interface BlockRepository {
    /** 여행의 블록 목록. 누가 바꾸면 새 목록이 다시 흘러온다 (실시간) */
    fun observeBlocks(tripId: String): Flow<List<Block>>

    /** 추가·수정 (같은 id 면 덮어씀) */
    suspend fun upsert(block: Block)

    suspend fun delete(tripId: String, blockId: String)

    /** 다른 사람과 겹치지 않는 새 블록 id */
    fun newId(tripId: String): String
}

/** 메모리에만 저장하는 가짜 저장소. 앱을 끄면 처음 상태로 돌아간다. */
class FakeBlockRepository(initial: List<Block> = emptyList()) : BlockRepository {
    private val all = MutableStateFlow(initial)

    override fun observeBlocks(tripId: String): Flow<List<Block>> =
        all.map { list -> list.filter { it.tripId == tripId } }

    override suspend fun upsert(block: Block) {
        all.value = all.value.filterNot { it.id == block.id && it.tripId == block.tripId } + block
    }

    override suspend fun delete(tripId: String, blockId: String) {
        all.value = all.value.filterNot { it.id == blockId && it.tripId == tripId }
    }

    override fun newId(tripId: String): String = newBlockId(all.value)
}
