package com.example.mbtrip.timeline.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mbtrip.timeline.data.BlockRepository
import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.model.Block
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 일정표 상태 관리 (MVVM).
 *  - blocks  : 저장소(Fake 또는 Firestore)에서 실시간으로 흘러오는 블록 목록. null = 불러오는 중
 *  - 규칙 검사(겹침·잠금·상태)는 TimelineLogic 의 함수를 그대로 쓰고, 통과하면 저장소에 쓴다.
 *  - 저장이 끝나면 저장소가 새 목록을 다시 흘려보내므로 화면이 자동으로 바뀐다.
 */
class TimelineViewModel(
    private val repository: BlockRepository,
    val tripId: String,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** 화면 아래 알림(스낵바)으로 보여줄 문구 */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val blocks: StateFlow<List<Block>?> = repository.observeBlocks(tripId)
        .map<List<Block>, List<Block>?> { it }
        .catch { e ->
            _messages.tryEmit("일정을 불러오지 못했어요: ${e.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val current: List<Block> get() = blocks.value.orEmpty()

    /** + 버튼으로 만든 초안 저장. 다른 사람과 겹치지 않게 id 를 저장소에서 새로 받는다 */
    fun add(draft: Block): EditResult {
        val withId = draft.copy(id = repository.newId(tripId))
        val r = addBlock(current, withId)
        if (r is EditResult.Ok) save(r.blocks.first { it.id == withId.id })
        return r
    }

    fun update(original: Block, edited: Block): EditResult {
        val r = updateBlock(current, original, edited)
        if (r is EditResult.Ok) save(r.blocks.first { it.id == edited.id })
        return r
    }

    fun delete(blockId: String): EditResult {
        val r = deleteBlock(current, blockId)
        if (r is EditResult.Ok) {
            viewModelScope.launch {
                runCatching { repository.delete(tripId, blockId) }
                    .onFailure { _messages.tryEmit("삭제하지 못했어요: ${it.message}") }
            }
        }
        return r
    }

    fun move(blockId: String, requestedStartMinutes: Int, window: IntRange): EditResult {
        val before = current.firstOrNull { it.id == blockId }
        val r = moveBlock(current, blockId, requestedStartMinutes, window)
        if (r is EditResult.Ok) {
            val after = r.blocks.firstOrNull { it.id == blockId }
            if (after != null && after != before) save(after)
        }
        return r
    }

    /** 테스트용: 빈 여행에 거북섬 1박 가짜 일정을 넣는다 (Firestore 처음 연결했을 때 확인용) */
    fun seedFakeData() {
        viewModelScope.launch {
            runCatching { FakeBlocks.blocks.forEach { repository.upsert(it.copy(tripId = tripId)) } }
                .onSuccess { _messages.tryEmit("가짜 일정을 넣었어요.") }
                .onFailure { _messages.tryEmit("가짜 일정을 넣지 못했어요: ${it.message}") }
        }
    }

    private fun save(block: Block) {
        viewModelScope.launch {
            runCatching { repository.upsert(block) }
                .onFailure { _messages.tryEmit("저장하지 못했어요: ${it.message}") }
        }
    }

    companion object {
        fun factory(repository: BlockRepository, tripId: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { TimelineViewModel(repository, tripId) } }
    }
}
