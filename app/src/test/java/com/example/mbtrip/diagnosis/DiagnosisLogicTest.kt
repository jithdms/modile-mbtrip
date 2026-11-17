package com.example.mbtrip.diagnosis

import org.junit.Assert.*
import org.junit.Test

class DiagnosisLogicTest {
    private fun answers(choice: DiagnosisChoice) = (0 until 10).associateWith { choice }

    @Test fun allJUsesZeroOnStoredAxis() {
        val result = TravelDiagnosis.calculate(answers(DiagnosisChoice.J))
        assertEquals(20, result.jScore)
        assertEquals(0, result.pScore)
        assertEquals(100, result.jPercent)
        assertEquals(0, result.firestoreJpScore)
        assertEquals("강한 J 성향", result.label)
    }

    @Test fun allPUsesOneHundredOnStoredAxis() {
        val result = TravelDiagnosis.calculate(answers(DiagnosisChoice.P))
        assertEquals(100, result.firestoreJpScore)
        assertEquals(0, result.jPercent)
        assertEquals("강한 P 성향", result.label)
    }

    @Test fun balancedAnswersSplitScoresEqually() {
        val result = TravelDiagnosis.calculate(answers(DiagnosisChoice.BALANCED))
        assertEquals(10, result.jScore)
        assertEquals(10, result.pScore)
        assertEquals(50, result.firestoreJpScore)
        assertEquals("균형형", result.label)
    }

    @Test fun everyReachableResultUsesTheCorrectLabelAndStorageAxis() {
        // A questionnaire can produce each J score from 0 through 20.
        for (jScore in 0..20) {
            val choices = List(jScore / 2) { DiagnosisChoice.J } +
                List(jScore % 2) { DiagnosisChoice.BALANCED }
            val map = (0 until 10).associateWith { choices.getOrElse(it) { DiagnosisChoice.P } }
            val result = TravelDiagnosis.calculate(map)
            val expectedJ = jScore * 5
            val expectedLabel = when {
                expectedJ <= 20 -> "강한 P 성향"
                expectedJ <= 40 -> "P 성향"
                expectedJ <= 60 -> "균형형"
                expectedJ <= 80 -> "J 성향"
                else -> "강한 J 성향"
            }
            assertEquals(expectedJ, result.jPercent)
            assertEquals(100 - expectedJ, result.firestoreJpScore)
            assertEquals(expectedLabel, result.label)
        }
    }

    @Test fun missingAnswersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TravelDiagnosis.calculate(answers(DiagnosisChoice.J) - 9)
        }
    }

    @Test fun tenAnswersWithAnInvalidIndexAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TravelDiagnosis.calculate((1..10).associateWith { DiagnosisChoice.J })
        }
    }

    @Test fun extraAnswersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TravelDiagnosis.calculate((0..10).associateWith { DiagnosisChoice.J })
        }
    }

    @Test fun companionAxisAndValidationAgreeWithStorage() {
        assertEquals(100, CompanionMember("j", "J", 0).jPercent)
        assertEquals(0, CompanionMember("p", "P", 100).jPercent)
        assertThrows(IllegalArgumentException::class.java) { CompanionMember("x", "X", -1) }
        assertThrows(IllegalArgumentException::class.java) { CompanionMember("x", "X", 101) }
    }
}
