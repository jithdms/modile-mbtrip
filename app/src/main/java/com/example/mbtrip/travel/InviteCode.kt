package com.example.mbtrip.travel

import java.security.SecureRandom
import java.util.Locale

object InviteCode {
    fun generate(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02X".format(it.toInt() and 255) }
    fun normalize(value: String): String = value.trim().replace("-", "").replace(" ", "").uppercase(Locale.ROOT)
        .also { require(Regex("[0-9A-F]{32}").matches(it)) { "초대 코드 32자리를 확인해주세요." } }
}
