package com.example.mbtrip.auth

import org.junit.Assert.*
import org.junit.Test

class AccountInputTest {
    @Test fun blankOrMalformedEmailRejected() {
        listOf("", "plain", "a@b", "a b@c.com").forEach { assertNotNull(AccountInput.validate(it, "secret")) }
    }
    @Test fun validEmailTrimmedButPasswordNotTrimmed() {
        assertNull(AccountInput.validate(" a@b.com ", " secret ", "여행자", " secret "))
        assertNotNull(AccountInput.validate("a@b.com", " secret ", "여행자", "secret"))
    }
    @Test fun registrationRequiresNameAndStrongEnoughMatchingPassword() {
        assertNotNull(AccountInput.validate("a@b.com", "12345", "나", "12345"))
        assertNotNull(AccountInput.validate("a@b.com", "123456", " ", "123456"))
        assertNotNull(AccountInput.validate("a@b.com", "123456", "a".repeat(21), "123456"))
        assertNull(AccountInput.validate("a@b.com", "123456", "나", "123456"))
    }
    @Test fun loginDoesNotApplyNewPasswordPolicyToExistingAccount() {
        assertNull(AccountInput.validate("a@b.com", "12345"))
        assertNotNull(AccountInput.validate("a@b.com", ""))
    }
}
