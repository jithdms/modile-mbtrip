package com.example.mbtrip.diagnosis

import kotlin.math.roundToInt

/**
 * 동행자들 성향으로 '적정 확정 · 자유 비율' 을 정한다. (jpScore: 0 = J, 100 = P)
 *
 *  1) 평균 J 비율로 기본값:  확정 % = 20 + 0.6 × 평균J%
 *     (진단 결과 표와 같은 범위 — 전원 강한 P → 확정 20%, 전원 강한 J → 80%, 반반 → 50%)
 *  2) 성향 차이(가장 J인 사람 − 가장 P인 사람)가 클수록 자유 시간을 조금 더 준다:
 *     차이 10%p 마다 확정 −2%  (최대 −10%)  → 서로 숨 돌릴 틈
 *  3) 5% 단위로 반올림, 20~80% 사이로 제한
 */
data class GroupRatio(
    val fixedPercent: Int,
    val avgJPercent: Int,
    val gapPercent: Int,
    val memberCount: Int,
) {
    val freePercent: Int get() = 100 - fixedPercent

    /** 화면용 한 줄 설명 */
    val reason: String
        get() = when {
            memberCount <= 1 -> "내 성향(J ${avgJPercent}%) 기준이에요. 친구를 초대하면 함께 계산해요."
            gapPercent >= 40 -> "평균 J ${avgJPercent}% · 성향 차이가 커요(${gapPercent}%p). 자유 시간을 조금 더 넣었어요."
            gapPercent >= 20 -> "평균 J ${avgJPercent}% · 성향 차이 ${gapPercent}%p 를 반영했어요."
            else -> "평균 J ${avgJPercent}% · 성향이 비슷해요."
        }

    companion object {
        fun of(jpScores: List<Int>): GroupRatio? {
            val scores = jpScores.filter { it in 0..100 }
            if (scores.isEmpty()) return null
            val jPercents = scores.map { 100 - it }
            val avgJ = jPercents.average()
            val gap = jPercents.max() - jPercents.min()
            val base = 20 + 0.6 * avgJ
            val adjusted = base - (gap / 10 * 2).coerceAtMost(10)
            val fixed = ((adjusted / 5).roundToInt() * 5).coerceIn(20, 80)
            return GroupRatio(fixed, avgJ.roundToInt(), gap, scores.size)
        }
    }
}
