package com.example.mbtrip.travel

import org.junit.Assert.*
import org.junit.Test

class InviteCodeTest {
    @Test fun generatedCodesHave128BitsAndCanBePasted() {
        val codes = List(100) { InviteCode.generate() }
        assertEquals(100, codes.toSet().size)
        codes.forEach { assertTrue(it.matches(Regex("[0-9A-F]{32}"))) }
        assertEquals(codes[0], InviteCode.normalize(" ${codes[0].lowercase().chunked(8).joinToString("-")} "))
    }
    @Test fun malformedInputsCannotBecomeDocumentPaths() {
        listOf("", "../trips", "a".repeat(31), "g".repeat(32), "a".repeat(33)).forEach {
            assertTrue(runCatching { InviteCode.normalize(it) }.isFailure)
        }
    }
}
