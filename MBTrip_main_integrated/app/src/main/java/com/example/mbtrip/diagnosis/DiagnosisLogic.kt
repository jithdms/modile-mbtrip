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

data class DiagnosisResult(
    val jScore: Int,
    val pScore: Int,
    val jPercent: Int,
    val pPercent: Int,
    val label: String
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

        val label = when (jPercent) {
            in 0..20 -> "강한 P 성향"
            in 21..40 -> "P 성향"
            in 41..60 -> "균형형"
            in 61..80 -> "J 성향"
            else -> "강한 J 성향"
        }

        return DiagnosisResult(jScore, pScore, jPercent, pPercent, label)
    }

    /** Simple descriptive guidance based on the spread of companions' J percentages. */
    fun groupSummary(selfJPercent: Int, companions: List<CompanionMember>): String {
        if (companions.isEmpty()) {
            return "동행자 진단 결과가 연결되면 성향 차이를 함께 확인할 수 있어요."
        }

        val scores = companions.map(CompanionMember::jPercent) + selfJPercent
        val spread = (scores.maxOrNull() ?: selfJPercent) - (scores.minOrNull() ?: selfJPercent)
        val guidance = when {
            spread <= 15 -> "성향 차이가 크지 않아요. 각자 편한 방식으로 일정을 정해도 좋아요."
            spread <= 35 -> "계획을 정할 부분과 현지에서 정할 부분을 나눠 합의해보세요."
            else -> "일정 운영 방식 차이가 클 수 있어요. 예약과 자유시간의 기준을 미리 맞춰보세요."
        }
        return "동행자와의 J 성향 차이는 최대 ${spread}%p예요. $guidance"
    }
}
