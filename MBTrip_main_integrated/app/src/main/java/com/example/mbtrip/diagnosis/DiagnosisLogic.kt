package com.example.mbtrip.diagnosis

import kotlin.math.roundToInt

/** A selected option contributes two points to one or both ends of the spectrum. */
enum class DiagnosisChoice(val jPoints: Int, val pPoints: Int) {
    J(2, 0),
    BALANCED(1, 1),
    P(0, 2)
}

data class DiagnosisQuestion(
    val area: String,
    val prompt: String,
    val options: List<String>
)

/** jpScore follows the Firestore Member convention: 0 = J, 100 = P. */
data class CompanionMember(
    val userId: String,
    val name: String,
    val jpScore: Int
) {
    init {
        require(jpScore in 0..100) { "jpScore must be between 0 and 100." }
    }

    val jPercent: Int get() = 100 - jpScore
}

/**
 * 진단 결과 유형 — 기준표의 '진단 결과 범위' (J 비율 기준 5구간).
 * 문구는 여기서만 고치면 결과 화면에 그대로 나온다.
 */
enum class DiagnosisType(
    val jRange: IntRange,
    val emoji: String,
    val title: String,     // 결과 이름
    val label: String,     // 짧은 성향 표시
    val summary: String,   // 한 줄 설명
    val traits: List<String>, // 이런 모습이 있어요
    val tip: String,       // 함께 일정 짤 때
    val fixedPercent: Int, // 이 사람에게 편한 확정:자유 비율 (확정 %)
) {
    STRONG_P(
        0..20, "🎈", "자유로운 즉흥 여행가", "강한 P 성향",
        "계획보다 그날의 분위기와 발견을 즐기는 여행가예요.",
        listOf("일정은 현지에서, 그때그때 정해요", "갑작스러운 변경도 오히려 즐거워요", "예약은 최대한 미뤄도 괜찮아요"),
        "꼭 필요한 예약(숙소·교통)만 함께 정하고, 나머지는 자유 블록으로 넉넉히 남겨 두세요.",
        20,
    ),
    P(
        21..40, "🧭", "유연한 탐험가", "P 성향",
        "큰 방향만 정하고 현지에서 유연하게 움직이는 편이에요.",
        listOf("가고 싶은 곳 몇 군데만 정해 둬요", "대안을 찾는 걸 어렵지 않게 생각해요", "자유시간이 생기면 주변을 둘러봐요"),
        "핵심 일정 1~2개만 확정하고, 그 사이를 자유 블록으로 이어 보세요.",
        35,
    ),
    BALANCED(
        41..60, "⚖️", "균형 잡힌 여행가", "균형형",
        "계획과 즉흥 사이에서 상황에 맞게 균형을 잡아요.",
        listOf("중요한 것만 미리 정해요", "후보를 몇 개 두고 당일에 골라요", "동행자 성향에 맞춰 조율을 잘해요"),
        "J·P 동행자 사이에서 다리 역할을 해 줄 수 있어요. 확정과 자유를 반반 섞어 보세요.",
        50,
    ),
    J(
        61..80, "🗺️", "든든한 계획러", "J 성향",
        "주요 일정과 예약은 미리 챙기고, 세부는 조금 열어 두는 편이에요.",
        listOf("숙소·교통·식당은 미리 예약해요", "일정이 어긋나면 순서를 다시 짜요", "대체 장소도 미리 찾아 둬요"),
        "동선과 예약을 맡아 주면 모두가 편해요. 하루 한두 칸은 자유 블록으로 비워 두세요.",
        65,
    ),
    STRONG_J(
        81..100, "📋", "철저한 여행 설계자", "강한 J 성향",
        "시간대별 동선과 예약까지 다 정해야 마음이 편한 여행가예요.",
        listOf("이동 시간까지 계산해 일정을 짜요", "계획이 틀어지면 다른 일정을 조정해 지켜요", "마지막 날까지 빈 시간이 없어요"),
        "확정 블록을 중심으로 짜되, 즉흥파 동행자를 위해 쉬는 시간을 꼭 남겨 주세요.",
        80,
    );

    val freePercent: Int get() = 100 - fixedPercent

    companion object {
        fun of(jPercent: Int): DiagnosisType =
            entries.firstOrNull { jPercent.coerceIn(0, 100) in it.jRange } ?: BALANCED
    }
}

data class DiagnosisResult(
    val jScore: Int,
    val pScore: Int,
    val jPercent: Int,
    val pPercent: Int,
    val label: String,
    val type: DiagnosisType = DiagnosisType.of(jPercent),
) {
    /** Value to save to Member.jpScore (0 = J, 100 = P). */
    val firestoreJpScore: Int get() = pPercent
}

object TravelDiagnosis {
    const val QUESTION_COUNT = 10

    val questions = listOf(
        DiagnosisQuestion(
            "계획성",
            "여행을 떠나기 전에 하루 일정을 어떻게 정하는 편인가요?",
            listOf(
                "이동 시간과 방문 장소까지 시간대별로 구체적으로 정한다.",
                "꼭 가야 할 장소와 대략적인 순서만 정한다.",
                "큰 계획만 정하고 구체적인 일정은 현지에서 정한다."
            )
        ),
        DiagnosisQuestion(
            "사전 계획과 준비",
            "여행 전에 숙소·교통·관광지 등을 얼마나 미리 정하는 편인가요?",
            listOf(
                "가능한 대부분의 항목을 미리 정하고 예약까지 완료한다.",
                "숙소나 교통처럼 중요한 것만 미리 정한다.",
                "현지 상황이나 당일 기분을 보고 결정한다."
            )
        ),
        DiagnosisQuestion(
            "일정 유지",
            "미리 계획했던 관광지를 방문하기 어려워졌다면 어떻게 하나요?",
            listOf(
                "다른 일정을 조정해서라도 원래 계획을 최대한 유지한다.",
                "중요도가 낮은 일정은 변경하고 일부 계획을 유지한다.",
                "원래 계획에 얽매이지 않고 새로운 장소를 찾아본다."
            )
        ),
        DiagnosisQuestion(
            "변화 대응",
            "예상보다 이동 시간이 길어져 다음 일정에 차질이 생겼다면 어떻게 하나요?",
            listOf(
                "기존 일정이 최대한 유지되도록 다른 일정을 조정한다.",
                "중요도가 낮은 일정부터 일부 제외한다.",
                "기존 일정은 크게 신경 쓰지 않고 그때 상황에 맞춰 움직인다."
            )
        ),
        DiagnosisQuestion(
            "결정과 확정",
            "여행 중 저녁 식당을 정한다면 어떻게 하는 편인가요?",
            listOf(
                "여행 전에 식당을 정하고 예약까지 해둔다.",
                "후보 식당을 몇 곳 정해두고 당일 선택한다.",
                "당일 분위기나 주변 상황을 보고 즉석에서 정한다."
            )
        ),
        DiagnosisQuestion(
            "결정 시점",
            "인기 관광지나 식당을 예약해야 한다면 언제 예약하는 편인가요?",
            listOf(
                "가능한 한 미리 예약해서 일정을 확정한다.",
                "일정과 가격을 확인한 후 적당한 시기에 예약한다.",
                "가능한 한 예약을 미루고 현지 상황을 지켜본다."
            )
        ),
        DiagnosisQuestion(
            "새로운 제안 수용",
            "여행 중 동행자가 갑자기 새로운 장소에 가자고 한다면?",
            listOf(
                "기존 일정에 영향을 주는지 확인하고 계획에 맞지 않으면 거절한다.",
                "기존 일정과 비교해 가능하면 일정에 추가한다.",
                "재미있어 보이면 기존 일정을 바꾸더라도 바로 가본다."
            )
        ),
        DiagnosisQuestion(
            "예상 밖의 변화",
            "예정되어 있던 관광지가 갑자기 휴무라면 어떻게 하나요?",
            listOf(
                "미리 찾아둔 대체 장소로 바로 이동한다.",
                "몇 가지 대안을 찾아본 후 가장 적절한 곳을 선택한다.",
                "주변을 둘러보면서 그때 마음에 드는 곳을 찾아간다."
            )
        ),
        DiagnosisQuestion(
            "즉흥성",
            "여행 중 예상보다 2시간 정도 자유시간이 생긴다면?",
            listOf(
                "미리 찾아둔 장소를 방문한다.",
                "후보 장소를 몇 곳 찾아보고 선택한다.",
                "특별한 계획 없이 주변을 돌아다니며 마음에 드는 곳을 찾는다."
            )
        ),
        DiagnosisQuestion(
            "일정 확정과 개방성",
            "여행 마지막 날 일정이 비어 있다면 어떻게 하나요?",
            listOf(
                "미리 방문할 장소와 시간까지 정해둔다.",
                "꼭 가고 싶은 장소만 정하고 나머지는 자유롭게 둔다.",
                "일정 없이 당일 기분이나 상황에 따라 결정한다."
            )
        )
    )

    /** Map keys are zero-based question indexes. All ten answers are required. */
    fun calculate(answers: Map<Int, DiagnosisChoice>): DiagnosisResult {
        require(answers.size == QUESTION_COUNT && (0 until QUESTION_COUNT).all(answers::containsKey)) {
            "모든 문항에 답변해야 결과를 계산할 수 있습니다."
        }

        val choices = (0 until QUESTION_COUNT).map { answers.getValue(it) }
        val jScore = choices.sumOf(DiagnosisChoice::jPoints)
        val pScore = choices.sumOf(DiagnosisChoice::pPoints)
        val total = jScore + pScore
        val jPercent = (jScore * 100.0 / total).roundToInt()
        val pPercent = 100 - jPercent

        // 기준표 '진단 결과 범위': J 비율 0~20 / 21~40 / 41~60 / 61~80 / 81~100
        val type = DiagnosisType.of(jPercent)
        return DiagnosisResult(jScore, pScore, jPercent, pPercent, type.label, type)
    }

    /** Simple descriptive guidance based on the spread of companions' J percentages. */
    fun groupSummary(selfJPercent: Int, companions: List<CompanionMember>): String {
        if (companions.isEmpty()) {
            return "동행자 진단 결과가 연결되면 성향 차이를 함께 확인할 수 있어요."
        }

        val scores = companions.map(CompanionMember::jPercent) + selfJPercent
        val spread = (scores.maxOrNull() ?: selfJPercent) - (scores.minOrNull() ?: selfJPercent)
        val ratio = ItineraryAllocator.ratioFor(scores.map { 100 - it })
        val guidance = when {
            spread <= 15 -> "성향 차이가 크지 않아요. 각자 편한 방식으로 일정을 정해도 좋아요."
            spread <= 35 -> "계획을 정할 부분과 현지에서 정할 부분을 나눠 합의해보세요."
            else -> "일정 운영 방식 차이가 클 수 있어요. 예약과 자유시간의 기준을 미리 맞춰보세요."
        }
        return "동행자와의 J 성향 차이는 최대 ${spread}%p예요. 추천 일정 비율은 확정 ${ratio.fixedPercent}% · 자유 ${ratio.freePercent}%예요. $guidance"
    }
}