package com.example.mbtrip.timeline.vote

import kotlinx.coroutines.delay

/**
 * 투표 모달(화면: 지소은) ↔ 투표 로직(저장·실시간·합의: 최재성) 연결 약속.
 *
 * 최재성 님은 이 interface 를 구현한 클래스를 만들어
 * TimelineDependencies.voteActions 에 넣으면 된다. (화면 코드는 바꿀 필요 없음)
 */
interface VoteActions {
    /**
     * 블록에 찬성/반대와 의견을 보낸다.
     * 반대일 때 comment 는 비어 있지 않다 (화면에서 막음).
     * 실패하면 Result.failure — 화면이 오류 문구를 보여준다.
     */
    suspend fun submitVote(tripId: String, blockId: String, choice: VoteChoice, comment: String): Result<Unit>
}

enum class VoteChoice { AGREE, DISAGREE }

/** 의견 보내기 버튼을 눌러도 되는지 — 반대는 사유 필수 (제안서 부록 A3) */
fun canSubmitVote(choice: VoteChoice?, comment: String): Boolean = when (choice) {
    null -> false
    VoteChoice.AGREE -> true
    VoteChoice.DISAGREE -> comment.isNotBlank()
}

/** 최재성 님 로직이 나오기 전까지 쓰는 가짜 — 0.3초 뒤 성공 */
object FakeVoteActions : VoteActions {
    val sent = mutableListOf<Triple<String, VoteChoice, String>>() // (blockId, choice, comment) 확인용

    override suspend fun submitVote(
        tripId: String, blockId: String, choice: VoteChoice, comment: String,
    ): Result<Unit> {
        delay(300)
        sent += Triple(blockId, choice, comment.trim())
        return Result.success(Unit)
    }
}
