package com.medibox.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object AppMotion {
    val DurationFast = 150.milliseconds
    val DurationNormal = 200.milliseconds
    val DurationSlow = 250.milliseconds
    val Easing: Easing = FastOutSlowInEasing
    val SlideDistance = 16.dp
}
