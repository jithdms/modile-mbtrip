package com.example.mbtrip

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.diagnosis.DiagnosisView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosisViewInstrumentedTest {
    private fun onMain(test: (DiagnosisView) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = android.view.ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_MBTrip)
            test(DiagnosisView(context))
        }
    }

    @Test fun resultRequiresAllAnswersAndRestartClearsProgress() = onMain { view ->
        val resultButton = view.findViewById<Button>(R.id.showResultButton)
        assertFalse(resultButton.isEnabled)
        val questions = view.findViewById<LinearLayout>(R.id.questionContainer)
        for (i in 0 until 9) questions.getChildAt(i).findViewById<TextView>(R.id.answerJ).performClick()
        assertFalse(resultButton.isEnabled)
        questions.getChildAt(9).findViewById<TextView>(R.id.answerJ).performClick()
        assertTrue(resultButton.isEnabled)
        resultButton.performClick()
        assertEquals(View.VISIBLE, view.findViewById<LinearLayout>(R.id.resultContainer).visibility)
        assertEquals("J 100% · P 0%", view.findViewById<TextView>(R.id.resultDominantPercent).text.toString())
        view.findViewById<Button>(R.id.restartButton).performClick()
        assertEquals(0, view.findViewById<ProgressBar>(R.id.questionProgress).progress)
        assertFalse(resultButton.isEnabled)
        assertEquals(View.GONE, view.findViewById<LinearLayout>(R.id.resultContainer).visibility)
    }

    @Test fun savingAndFailurePreventContinuingUntilRetrySucceeds() = onMain { view ->
        view.configure("테스터", onContinue = {})
        val continueButton = view.findViewById<Button>(R.id.continueButton)
        view.setSaveState(true, null)
        assertFalse(continueButton.isEnabled)
        assertFalse(view.findViewById<Button>(R.id.restartButton).isEnabled)
        view.setSaveState(false, "저장 실패")
        assertFalse(continueButton.isEnabled)
        view.setSaveState(false, null)
        assertTrue(continueButton.isEnabled)
    }
}
