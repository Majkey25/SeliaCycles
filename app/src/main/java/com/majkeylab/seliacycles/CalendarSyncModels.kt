package com.majkeylab.seliacycles

import java.security.SecureRandom
import java.util.Base64

fun DayLog.toPartnerPayload(): Map<String, Any> = mapOf(
    "day" to day.toString(),
    "bleeding" to bleeding,
    "flow" to flow.name,
)

object PartnerInviteToken {
    private val pattern = Regex("^[A-Za-z0-9_-]{22}$")

    fun generate(): String = encode(ByteArray(16).also(SecureRandom()::nextBytes))

    fun encode(bytes: ByteArray): String {
        require(bytes.size == 16)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun normalize(value: String): String = value.trim().also { require(it.matches(pattern)) }
}
