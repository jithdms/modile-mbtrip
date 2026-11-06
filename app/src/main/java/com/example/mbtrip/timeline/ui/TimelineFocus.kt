package com.example.mbtrip.timeline.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 다른 화면(알림 등)에서 "일정표의 이 블록을 보여줘" 라고 부탁하는 곳.
 * 일정표가 이걸 보고 → 그 날짜 탭으로 바꾸고 → 블록까지 스크롤하고 → 몇 초 동안 테두리를 깜빡인다.
 */
object TimelineFocus {
    private val _request = MutableStateFlow<String?>(null)
    val request: StateFlow<String?> = _request.asStateFlow()

    fun focus(blockId: String) {
        _request.value = blockId
    }

    /** 일정표가 부탁을 받았으면 지운다 (한 번만 강조) */
    internal fun consume() {
        _request.value = null
    }
}
