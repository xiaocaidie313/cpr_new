package com.example.cpr_new.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** 进行态布局预留尺寸：为顶栏/底栏留出空间，避免内容挤压与点击穿透。 */
object CoachLayoutMetrics {
    val TopBarHeight = 48.dp
    val VoiceBarCollapsed = 52.dp
    val VoiceBarEmergencyRow = 36.dp
    val VoiceBarExpanded = 88.dp
    val ScreenHorizontal = 16.dp
    val ChromeGap = 8.dp
}

@Composable
fun coachContentPadding(inputExpanded: Boolean = false): PaddingValues {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBar = if (inputExpanded) {
        CoachLayoutMetrics.VoiceBarExpanded + CoachLayoutMetrics.ChromeGap
    } else {
        CoachLayoutMetrics.VoiceBarCollapsed +
            CoachLayoutMetrics.VoiceBarEmergencyRow +
            CoachLayoutMetrics.ChromeGap * 2
    }
    return PaddingValues(
        top = statusTop + CoachLayoutMetrics.TopBarHeight + CoachLayoutMetrics.ChromeGap,
        bottom = navBottom + bottomBar + CoachLayoutMetrics.ChromeGap,
        start = CoachLayoutMetrics.ScreenHorizontal,
        end = CoachLayoutMetrics.ScreenHorizontal,
    )
}
