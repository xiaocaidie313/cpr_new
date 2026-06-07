package com.example.cpr_new.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpr_new.feature.session.CprSessionState
import com.example.cpr_new.feature.session.MicState
import com.example.cpr_new.feature.session.statusLabel
import com.example.cpr_new.ui.CoachPalette
import com.example.cpr_new.ui.agentStageIndex
import com.example.cpr_new.ui.connectionChip
import com.example.cpr_new.ui.micIndicatorColor
import com.example.cpr_new.ui.primaryGuidanceText
import com.example.cpr_new.ui.stageStatusLabel

@Composable
fun AgentTopStatusBar(state: CprSessionState, modifier: Modifier = Modifier) {
    val (chipLabel, chipColor) = connectionChip(state.agentConnected, state.liveWsConnected)
    Surface(
        color = CoachPalette.TopBar,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip(chipLabel, chipColor)
            Text(
                text = stageStatusLabel(state.agentStage),
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AgentFlowRail(agentStage: String?, modifier: Modifier = Modifier) {
    val currentIndex = agentStageIndex(agentStage)
    Surface(color = CoachPalette.Rail, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (0..9).forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(if (index <= currentIndex) Color(0xFF22C55E) else Color(0xFF334155)),
                )
            }
        }
    }
}

@Composable
fun CoachGuidancePanel(
    state: CprSessionState,
    modifier: Modifier = Modifier,
) {
    val mainText = primaryGuidanceText(
        state.latestGuidance?.messageText.orEmpty(),
        state.agentStage,
    )
    Surface(
        color = CoachPalette.Panel,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = mainText,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            CompactQualityDial(score = state.qualityScore, agentStage = state.agentStage)
        }
    }
}

@Composable
fun CompactQualityDial(score: Int, agentStage: String?, modifier: Modifier = Modifier) {
    val show = agentStage?.startsWith("S6") == true ||
        agentStage?.startsWith("S7") == true ||
        agentStage?.startsWith("S8") == true ||
        score > 0
    if (!show) return

    val (label, color) = when {
        score >= 80 -> "好" to Color(0xFF22C55E)
        score >= 60 -> "稳" to Color(0xFF38BDF8)
        score > 0 -> "调" to Color(0xFFF59E0B)
        else -> "检测中" to Color(0xFF94A3B8)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111827))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("质量", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(
            text = if (score > 0) score.toString() else "--",
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
        )
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VoiceStatusChip(state: CprSessionState, modifier: Modifier = Modifier) {
    val label = state.micState.statusLabel(state.liveWsConnected)
    if (label.isBlank()) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CoachPalette.TopBar)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MicLevelDot(micState = state.micState, level = state.micLevel)
        Text(label, color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (state.partialTranscript.isNotBlank()) {
            Text(
                text = "「${state.partialTranscript}」",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun MicLevelDot(micState: MicState, level: Float) {
    val color = micIndicatorColor(micState)
    val size = 12.dp + (8.dp * level.coerceIn(0f, 1f))
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun CoachBottomBar(
    onDialEmergency: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = CoachPalette.Panel,
        shape = RoundedCornerShape(26.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoachActionButton(
                text = "拨打 120",
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f),
                onClick = onDialEmergency,
            )
            CoachActionButton(
                text = "结束",
                color = Color(0xFF334155),
                modifier = Modifier.weight(1f),
                onClick = onStop,
            )
        }
    }
}

@Composable
private fun CoachActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
