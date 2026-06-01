package com.example.cpr_new.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpr_new.core.contract.HandPosition
import com.example.cpr_new.core.contract.PerceptionEvent
import com.example.cpr_new.ui.EmergencyPalette

/**
 * 急救 UI 可复用组件集合。
 *
 * 全部为无状态（stateless）组件：只接收数据、回调，不持有业务状态，
 * 便于预览、测试与复用。配色统一走 [EmergencyPalette]。
 */

/** 顶部阶段 + 兜底告警横幅。 */
@Composable
fun StatusBanner(
    phaseLabel: String,
    incident: String?,
    onDismissIncident: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = phaseLabel,
            color = EmergencyPalette.OnSurfaceMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
        // 兜底提示用动画淡入淡出，醒目但不打断主视图。
        AnimatedVisibility(visible = incident != null) {
            // 整条横幅可点击关闭。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmergencyPalette.Warn)
                    .clickable(onClick = onDismissIncident)
                    .padding(12.dp),
            ) {
                Text(
                    text = incident.orEmpty() + "（点击关闭）",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 大字指导卡片 —— 急救主视图核心（动词开头、超大字号）。 */
@Composable
fun GuidanceCard(displayText: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EmergencyPalette.Surface)
            .padding(vertical = 28.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText.ifBlank { "准备就绪" },
            color = EmergencyPalette.OnSurface,
            fontSize = 44.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 节拍器脉冲指示器：根据 bpm 做缩放呼吸动画。
 * 用动画近似节拍视觉，不强依赖逐拍事件，性能友好。
 */
@Composable
fun MetronomeIndicator(bpm: Int, running: Boolean, modifier: Modifier = Modifier) {
    val periodMs = (60_000 / bpm.coerceAtLeast(1))
    val transition = rememberInfiniteTransition(label = "metronome")
    val scale by transition.animateFloat(
        initialValue = if (running) 0.7f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs / 2),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(if (running) scale else 1f)
                .clip(CircleShape)
                .background(EmergencyPalette.Accent.copy(alpha = if (running) 1f else 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$bpm",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = "次/分 节拍",
            color = EmergencyPalette.OnSurfaceMuted,
            fontSize = 13.sp,
        )
    }
}

/** 质量仪表盘：综合分 + 频率 + 手位三块指标。 */
@Composable
fun QualityDashboard(
    qualityScore: Int,
    perception: PerceptionEvent?,
    rateInRange: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EmergencyPalette.Surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MetricBlock(
            label = "质量分",
            value = "$qualityScore",
            color = scoreColor(qualityScore),
        )
        MetricBlock(
            label = "频率(次/分)",
            value = perception?.compressionRate?.let { "%.0f".format(it) } ?: "--",
            color = if (rateInRange) EmergencyPalette.Good else EmergencyPalette.Warn,
        )
        MetricBlock(
            label = "手位",
            value = handLabel(perception?.handPosition),
            color = if (perception?.handPosition == HandPosition.CORRECT)
                EmergencyPalette.Good else EmergencyPalette.Warn,
        )
    }
}

@Composable
private fun MetricBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(label, color = EmergencyPalette.OnSurfaceMuted, fontSize = 13.sp)
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 75 -> EmergencyPalette.Good
    score >= 45 -> EmergencyPalette.Warn
    else -> EmergencyPalette.Danger
}

private fun handLabel(position: HandPosition?): String = when (position) {
    HandPosition.CORRECT -> "正确"
    HandPosition.TOO_HIGH -> "偏高"
    HandPosition.TOO_LOW -> "偏低"
    HandPosition.OFF_CENTER -> "偏移"
    HandPosition.UNKNOWN, null -> "--"
}
