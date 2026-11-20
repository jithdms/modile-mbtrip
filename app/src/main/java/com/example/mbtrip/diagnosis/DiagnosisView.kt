package com.example.mbtrip.diagnosis

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
    // init 안에서 애니메이션이 쓰므로 init 보다 먼저 선언
    private val density = context.resources.displayMetrics.density

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

        // ── 움직임 ──
        listOf(R.id.diagnosisHeroIcon, R.id.diagnosisTitle, R.id.diagnosisSubtitle, R.id.progressText, R.id.questionProgress)
            .forEachIndexed { i, id -> riseIn(findViewById(id), i) }
        for (i in 0 until questionContainer.childCount) riseIn(questionContainer.getChildAt(i), i + 4)
        listOf<View>(showResultButton, findViewById<View>(R.id.restartButton), findViewById<View>(R.id.continueButton))
            .forEach { bounceOnPress(it) }
    }

    /* ───────── 애니메이션 도우미 ───────── */

    /** 아래에서 떠오르며 나타남 (order 가 클수록 늦게) */
    private fun riseIn(v: View, order: Int, distanceDp: Float = 12f) {
        v.alpha = 0f
        v.translationY = distanceDp * density
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(order * 60L)
            .setDuration(480)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()
    }

    /** 고른 답 칸이 살짝 눌렸다 돌아옴 */
    private fun pop(v: View, to: Float = 0.97f) {
        v.animate().cancel()
        v.animate().scaleX(to).scaleY(to).setDuration(80).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(DecelerateInterpolator()).start()
        }.start()
    }

    /** 누르는 동안 살짝 작아진다 (클릭은 그대로 전달) */
    @SuppressLint("ClickableViewAccessibility")
    private fun bounceOnPress(v: View) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> if (view.isEnabled) view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(DecelerateInterpolator()).start()
            }
            false
        }
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
                    val firstTime = selectedAnswers[index] == null
                    selectedAnswers[index] = choice
                    updateOptionStyles(card, selectedAnswers[index])
                    pop(option)
                    updateProgress()
                    // 처음 고른 문항이면 다음 문항으로 부드럽게 내려간다
                    if (firstTime) scrollToNext(index)
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

    /**
     * 아직 답하지 않은 다음 문항을 화면 **가운데**로 스크롤한다.
     * (다 답했으면 [진단 결과 보기] 버튼을 가운데로)
     */
    private fun scrollToNext(index: Int) {
        val count = questionContainer.childCount
        // 지금 문항 뒤에서 먼저 찾고, 없으면 앞쪽에서 빠뜨린 문항을 찾는다
        val nextIndex = ((index + 1 until count) + (0 until index)).firstOrNull { selectedAnswers[it] == null }
        val target: View = nextIndex?.let { questionContainer.getChildAt(it) } ?: showResultButton
        postDelayed({ centerInScroll(target) }, 220)
    }

    /** target 의 가운데가 스크롤 화면의 가운데에 오도록 */
    private fun centerInScroll(target: View) {
        val content = diagnosisScroll.getChildAt(0) ?: return
        // target 이 스크롤 내용 맨 위에서 얼마나 떨어져 있는지 (부모를 따라 올라가며 더한다)
        var top = 0
        var v: View? = target
        while (v != null && v !== content) {
            top += v.top
            v = v.parent as? View
        }
        val y = top + target.height / 2 - diagnosisScroll.height / 2
        val maxY = (content.height - diagnosisScroll.height).coerceAtLeast(0)
        diagnosisScroll.smoothScrollTo(0, y.coerceIn(0, maxY))
    }

    private fun updateProgress() {
        val answered = selectedAnswers.size
        progressText.text = "$answered / ${TravelDiagnosis.QUESTION_COUNT} 문항 응답"
        // 막대는 부드럽게 차오른다 (max = 1000)
        questionProgress.setProgress(answered * 1000 / TravelDiagnosis.QUESTION_COUNT, true)
        val wasEnabled = showResultButton.isEnabled
        showResultButton.isEnabled = answered == TravelDiagnosis.QUESTION_COUNT
        showResultButton.alpha = 1f
        // 다 답하면 결과 버튼이 살짝 눌렸다 돌아오며 알려 준다
        if (!wasEnabled && showResultButton.isEnabled) pop(showResultButton)
    }

    private fun showResult() {
        if (selectedAnswers.size != TravelDiagnosis.QUESTION_COUNT) return
        val result = TravelDiagnosis.calculate(selectedAnswers)
        onResult?.invoke(result)
        val greeting = findViewById<TextView>(R.id.resultGreeting)
        greeting.text = "${displayName}님의 성향은..."
        val type = result.type
        // 기준표 구간별 결과: 이름 · 성향
        findViewById<TextView>(R.id.resultLabel).text = "${type.title}\n(${type.label})"
        // 숫자가 0 에서 올라간다
        val percentView = findViewById<TextView>(R.id.resultPercent)
        ValueAnimator.ofInt(0, result.jPercent).apply {
            duration = 1100
            startDelay = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val j = it.animatedValue as Int
                percentView.text = "J ${j}%  ·  P ${100 - j}%"
            }
            start()
        }
        // Stored Member.jpScore uses the opposite axis: 0 = J, 100 = P.
        val spectrum = findViewById<ProgressBar>(R.id.resultSpectrum)
        spectrum.progress = 0
        ObjectAnimator.ofInt(spectrum, "progress", 0, result.jPercent * 10).apply {
            duration = 1100
            startDelay = 350
            interpolator = DecelerateInterpolator()
            start()
        }
        findViewById<TextView>(R.id.resultScores).text =
            "J 점수 ${result.jScore} / 20 · P 점수 ${result.pScore} / 20"

        // 구간별 설명 (동행자 비교는 빼고, 내 유형 설명으로)
        findViewById<TextView>(R.id.companionScores).text = buildString {
            append(type.summary)
            append("\n\n이런 모습이 있어요\n")
            type.traits.forEach { append("· ").append(it).append("\n") }
        }.trimEnd()
        findViewById<TextView>(R.id.groupSummary).text =
            "함께 일정 짤 때\n${type.tip}\n\n편한 일정 비율  확정 ${type.fixedPercent}% · 자유 ${type.freePercent}%"

        findViewById<TextView>(R.id.diagnosisTitle).visibility = View.GONE
        findViewById<TextView>(R.id.diagnosisSubtitle).visibility = View.GONE
        progressText.visibility = View.GONE
        questionProgress.visibility = View.GONE
        questionContainer.visibility = View.GONE
        showResultButton.visibility = View.GONE
        resultContainer.visibility = View.VISIBLE
        diagnosisScroll.smoothScrollTo(0, 0)
        // 결과가 위에서부터 차례로 떠오르고, 유형 이름은 톡 커진다
        for (i in 0 until resultContainer.childCount) riseIn(resultContainer.getChildAt(i), i)
        val label = findViewById<View>(R.id.resultLabel)
        label.scaleX = 0.96f
        label.scaleY = 0.96f
        label.animate().scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(300)
            .setInterpolator(DecelerateInterpolator()).start()
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
        for (i in 0 until questionContainer.childCount) riseIn(questionContainer.getChildAt(i), i)
    }
}
