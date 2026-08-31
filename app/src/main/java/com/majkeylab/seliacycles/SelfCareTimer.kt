package com.majkeylab.seliacycles

object SelfCareTimer {
    fun remainingSeconds(targetMillis: Long, nowMillis: Long): Int =
        ((targetMillis - nowMillis).coerceAtLeast(0) + 999L).div(1_000L).toInt()
}
