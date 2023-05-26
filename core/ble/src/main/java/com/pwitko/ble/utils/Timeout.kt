package com.pwitko.ble.utils

import java.util.concurrent.TimeUnit

data class Timeout(
    @androidx.annotation.IntRange(from = 0) val value: Int,
    val unit: TimeUnit
)
