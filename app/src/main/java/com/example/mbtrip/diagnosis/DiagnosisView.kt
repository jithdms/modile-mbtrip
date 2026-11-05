package com.example.mbtrip.diagnosis

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.example.mbtrip.R

/** XML-backed diagnosis flow. It can be hosted from a Compose screen with DiagnosisScreen. */
class DiagnosisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val selectedAnswers = mutableMapOf<Int, DiagnosisChoice>()
    private var displayName: String = "나"
    private var companions: List<CompanionMember> = emptyList()
    private var onContinue: (() -> Unit)? = null
    private var onResult: ((DiagnosisResult) -> Unit)? = null

    private val questionContainer: LinearLayout
    private val questionProgress: ProgressBar
    private val progressText: TextView
    private val showResultButton: Button
    private val resultContainer: LinearLayout
    private val diagnosisScroll: ScrollView

    init {
        LayoutInflater.from(context).inflate(R.layout.diagnosis_screen, this, true)
        questionContainer = findViewById(R.id.questionContainer)
        questionProgress = findViewById(R.id.questionProgress)
        progressText = findViewById(R.id.progressText)
        showResultButton = findViewById(R.id.showResultButton)
        resultContainer = findViewById(R.id.resultContainer)
        diagnosisScroll = findViewById(R.id.diagnosisScroll)

        buildQuestionCards()
        showResultButton.setOnClickListener { showResult() }
        findViewById<Button>(R.id.restartButton).setOnClickListener { restart() }
    }

    fun configure(
        displayName: String,
        companions: List<CompanionMember>,
        onContinue: (() -> Unit)? = null,
        onResult: ((DiagnosisResult) -> Unit)? = null
    ) {
        this.displayName = displayName.ifBlank { "나" }
        this.companions = companions
        this.onContinue = onContinue
        this.onResult = onResult
        findViewById<Button>(R.id.continueButton).visibility =
            if (onContinue == null) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.continueButton).setOnClickListener { this.onContinue?.invoke() }
    }

    private fun buildQuestionCards() {
        val inflater = LayoutInflater.from(context)
        TravelDiagnosis.questions.forEachIndexed { index, question ->
            val card = inflater.inflate(R.layout.item_diagnosis_question, questionContainer, false)
            card.findViewById<TextView>(R.id.questionNumber).text = "Q${index + 1} · ${question.area}"
            card.findViewById<TextView>(R.id.questionPrompt).text = question.prompt

            val choices = listOf(
                DiagnosisChoice.J to R.id.answerJ,
                DiagnosisChoice.BALANCED to R.id.answerBalanced,
                DiagnosisChoice.P to R.id.answerP
            )
            choices.forEachIndexed { optionIndex, (choice, viewId) ->
                val option = card.findViewById<TextView>(viewId)
                option.text = question.options[optionIndex]
                option.setOnClickListener {
                    selectedAnswers[index] = choice
                    updateOptionStyles(card, selectedAnswers[index])
                    updateProgress()
                }
            }
            questionContainer.addView(card)
        }
    }

    private fun updateOptionStyles(card: View, selected: DiagnosisChoice?) {
        val optionViews = listOf(
            DiagnosisChoice.J to card.findViewById<TextView>(R.id.answerJ),
            DiagnosisChoice.BALANCED to card.findViewById<TextView>(R.id.answerBalanced),
            DiagnosisChoice.P to card.findViewById<TextView>(R.id.answerP)
        )
        optionViews.forEach { (choice, view) ->
            val background = if (choice == selected) {
                R.drawable.diagnosis_answer_selected
            } else {
                R.drawable.diagnosis_answer_unselected
            }
            view.setBackgroundResource(background)
        }
    }

    private fun updateProgress() {
        val answered = selectedAnswers.size
        progressText.text = "$answered / ${TravelDiagnosis.QUESTION_COUNT} 문항 응답"
        questionProgress.progress = answered
        showResultButton.isEnabled = answered == TravelDiagnosis.QUESTION_COUNT
        showResultButton.alpha = if (showResultButton.isEnabled) 1f else 0.5f
    }

    private fun showResult() {
        if (selectedAnswers.size != TravelDiagnosis.QUESTION_COUNT) return
        val result = TravelDiagnosis.calculate(selectedAnswers)
        onResult?.invoke(result)
        val greeting = findViewById<TextView>(R.id.resultGreeting)
        greeting.text = "${displayName}님의 성향은..."
        findViewById<TextView>(R.id.resultLabel).text = "\"${result.label}\""
        findViewById<TextView>(R.id.resultPercent).text = "J ${result.jPercent}%  ↔  P ${result.pPercent}%"
        // Stored Member.jpScore uses the opposite axis: 0 = J, 100 = P.
        findViewById<ProgressBar>(R.id.resultSpectrum).progress = result.jPercent
        findViewById<TextView>(R.id.resultScores).text =
            "J 점수 ${result.jScore} / 20 · P 점수 ${result.pScore} / 20"

        val companionText = if (companions.isEmpty()) {
            ""
        } else {
            companions.joinToString(separator = "\n") { companion ->
                "${companion.name}: J ${companion.jPercent}% · P ${companion.jpScore}%"
            }
        }
        findViewById<TextView>(R.id.companionScores).text = companionText
        findViewById<TextView>(R.id.groupSummary).text =
            TravelDiagnosis.groupSummary(result.jPercent, companions)

        findViewById<TextView>(R.id.diagnosisTitle).visibility = View.GONE
        findViewById<TextView>(R.id.diagnosisSubtitle).visibility = View.GONE
        progressText.visibility = View.GONE
        questionProgress.visibility = View.GONE
        questionContainer.visibility = View.GONE
        showResultButton.visibility = View.GONE
        resultContainer.visibility = View.VISIBLE
        diagnosisScroll.smoothScrollTo(0, 0)
    }

    private fun restart() {
        selectedAnswers.clear()
        for (index in 0 until questionContainer.childCount) {
            updateOptionStyles(questionContainer.getChildAt(index), null)
        }
        progressText.visibility = View.VISIBLE
        questionProgress.visibility = View.VISIBLE
        questionContainer.visibility = View.VISIBLE
        showResultButton.visibility = View.VISIBLE
        findViewById<TextView>(R.id.diagnosisTitle).visibility = View.VISIBLE
        findViewById<TextView>(R.id.diagnosisSubtitle).visibility = View.VISIBLE
        resultContainer.visibility = View.GONE
        updateProgress()
        questionContainer.post { diagnosisScroll.smoothScrollTo(0, 0) }
    }
}
