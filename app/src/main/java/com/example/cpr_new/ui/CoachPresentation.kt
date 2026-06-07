package com.example.cpr_new.ui

import androidx.compose.ui.graphics.Color
import com.example.cpr_new.feature.session.MicState

fun stageStatusLabel(agentStage: String?): String =
    when {
        agentStage.isNullOrBlank() -> "待命"
        agentStage.startsWith("S1") -> "安全"
        agentStage.startsWith("S2") -> "反应"
        agentStage.startsWith("S3") -> "呼吸"
        agentStage.startsWith("S4") || agentStage.startsWith("S5") -> "呼叫"
        agentStage.startsWith("S6") -> "准备"
        agentStage.startsWith("S7") -> "按压"
        agentStage.startsWith("S8") -> "协助"
        agentStage.startsWith("S9") -> "交接"
        else -> "进行"
    }

fun agentStageIndex(agentStage: String?): Int =
    agentStage?.let { Regex("""S(\d+)""").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() } ?: 0

fun connectionChip(agentConnected: Boolean, liveWsConnected: Boolean): Pair<String, Color> =
    when {
        agentConnected && liveWsConnected -> "在线" to Color(0xFF22C55E)
        agentConnected -> "HTTP" to Color(0xFF38BDF8)
        else -> "离线" to Color(0xFFEF4444)
    }

fun primaryGuidanceText(messageText: String, agentStage: String?): String =
    when {
        messageText.isNotBlank() -> messageText
        agentStage.isNullOrBlank() -> "准备急救"
        agentStage.startsWith("S7") -> "继续按压"
        else -> "听语音指引"
    }

fun micIndicatorColor(micState: MicState): Color = when (micState) {
    MicState.Speaking -> Color(0xFFA78BFA)
    MicState.Capturing -> Color(0xFF22C55E)
    MicState.Listening -> Color(0xFF38BDF8)
    MicState.Off -> Color(0xFF64748B)
    MicState.Idle -> Color(0xFF334155)
}
