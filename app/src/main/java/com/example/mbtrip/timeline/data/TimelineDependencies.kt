package com.example.mbtrip.timeline.data

import com.example.mbtrip.timeline.fake.FakeBlocks
import com.example.mbtrip.timeline.vote.FakeVoteActions
import com.example.mbtrip.timeline.vote.VoteActions

/**
 * 일정표가 쓰는 저장소를 한 곳에서 고른다.
 *
 * ▶ Firebase 설정이 끝나면 (가이드 참고)
 *   1. FirestoreBlockRepository.kt 파일을 이 폴더(data/)에 넣고
 *   2. 아래 blockRepository 줄을 `FirestoreBlockRepository()` 로 바꾸기
 *
 * ▶ 최재성 님 투표 로직이 나오면 voteActions 를 그 클래스로 바꾸기
 */
object TimelineDependencies {
    var blockRepository: BlockRepository = FirestoreBlockRepository()
    var voteActions: VoteActions = FakeVoteActions
}
