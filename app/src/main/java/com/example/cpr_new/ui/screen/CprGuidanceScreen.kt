package com.example.cpr_new.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpr_new.core.di.AgentBackend
import com.example.cpr_new.core.di.ServiceLocator
import com.example.cpr_new.core.contract.FrameSink
import com.example.cpr_new.core.contract.GuidancePriority
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.feature.session.CprSessionState
import com.example.cpr_new.hardware.camera.CameraPreview
import com.example.cpr_new.ui.CoachPalette
import com.example.cpr_new.ui.EmergencyPalette
import com.example.cpr_new.ui.component.AgentFlowRail
import com.example.cpr_new.ui.component.AgentTopStatusBar
import com.example.cpr_new.ui.component.CoachBottomBar
import com.example.cpr_new.ui.component.CoachGuidancePanel
import com.example.cpr_new.ui.component.MetronomeIndicator
import com.example.cpr_new.ui.component.StatusBanner
import com.example.cpr_new.ui.component.VoiceStatusChip

/**
 * 急救指导主界面 —— 急救 UI 顶层。
 *
 * 无状态屏幕：所有数据来自 [state]，所有交互通过回调上抛给 ViewModel。
 * 这样 UI 可独立预览/测试，也方便后续替换为多页导航。
 */
@Composable
fun CprGuidanceScreen(
    state: CprSessionState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDialEmergency: () -> Unit,
    onDismissIncident: () -> Unit,
    onDismissReport: () -> Unit,
    onPrimaryButton: () -> Unit = {},
    onQuickReply: (String) -> Unit = {},
    showQuickReplies: Boolean = false,
    modifier: Modifier = Modifier,
    cameraGranted: Boolean = false,
    frameSink: FrameSink? = null,
) {
    val rootModifier = modifier.fillMaxSize().then(
        if (state.isActive) {
            Modifier.background(CoachPalette.Background)
        } else {
            Modifier.background(
                Brush.verticalGradient(
                    listOf(EmergencyPalette.BackgroundTop, EmergencyPalette.BackgroundBottom),
                ),
            ).padding(horizontal = 16.dp, vertical = 16.dp)
        },
    )
    Box(modifier = rootModifier) {
        if (!state.isActive) {
            IdleContent(state = state, onStart = onStart, onDialEmergency = onDialEmergency)
        } else {
            ActiveContent(
                state = state,
                onStop = onStop,
                onDialEmergency = onDialEmergency,
                onPrimaryButton = onPrimaryButton,
                onQuickReply = onQuickReply,
                showQuickReplies = showQuickReplies,
                cameraGranted = cameraGranted,
                frameSink = frameSink,
            )
        }

        // 兜底告警悬浮在顶部：出现/消失都不挤压主内容布局。
        if (state.isActive) {
            StatusBanner(
                incident = state.incidentBanner,
                onDismissIncident = onDismissIncident,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        // 会话结束后弹出交接报告。
        state.handoverReport?.let { report ->
            HandoverDialog(report = report, onDismiss = onDismissReport)
        }
    }
}

/** 待命态：脉动心形英雄 + 大号开始按钮 + 三步预览 + 即时呼救入口。 */
@Composable
private fun IdleContent(
    state: CprSessionState,
    onStart: () -> Unit,
    onDialEmergency: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        PulsingHeart()
        Spacer(Modifier.height(28.dp))

        Text(
            text = "FirstAid Copilot",
            color = EmergencyPalette.OnSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "对准胸部，开始即可获得实时 CPR 指导",
            color = EmergencyPalette.OnSurfaceMuted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(28.dp))
        StepsPreview()

        if (state.handoverReport != null) {
            Text(
                text = "上次急救报告已生成",
                color = EmergencyPalette.OnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        BigButton(
            text = "开始急救",
            container = EmergencyPalette.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDialEmergency,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, EmergencyPalette.Danger),
        ) {
            Text(
                text = "立即拨打 120",
                color = EmergencyPalette.Danger,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

/** 待命页脉动心形：呼吸式缩放 + 双层光环，暗示“心跳/生命”。 */
@Composable
private fun PulsingHeart() {
    val transition = rememberInfiniteTransition(label = "heart")
    val scale by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "beat",
    )
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            drawCircle(
                color = EmergencyPalette.Primary.copy(alpha = 0.12f),
                radius = size.minDimension / 2,
            )
            drawCircle(
                color = EmergencyPalette.Primary.copy(alpha = 0.4f),
                radius = size.minDimension / 2 - 12.dp.toPx(),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        Text(
            text = "♥",
            color = EmergencyPalette.Primary,
            fontSize = 72.sp,
            modifier = Modifier.scale(scale),
        )
    }
}

/** 三步流程预览，降低首次使用的心理门槛。 */
@Composable
private fun StepsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EmergencyPalette.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepRow("1", "确认环境安全，轻拍呼叫患者")
        StepRow("2", "无反应无呼吸 → 立即拨打 120")
        StepRow("3", "跟随节拍，用力快速按压胸部")
    }
}

@Composable
private fun StepRow(index: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EmergencyPalette.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(index, color = EmergencyPalette.Accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = EmergencyPalette.OnSurface, fontSize = 15.sp)
    }
}

/** 进行态：仿 first-aid 全屏相机 + 半透明教练层。 */
@Composable
private fun ActiveContent(
    state: CprSessionState,
    onStop: () -> Unit,
    onDialEmergency: () -> Unit,
    onPrimaryButton: () -> Unit,
    onQuickReply: (String) -> Unit,
    showQuickReplies: Boolean,
    cameraGranted: Boolean,
    frameSink: FrameSink?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            enabled = cameraGranted,
            sessionId = state.sessionId,
            frameSink = frameSink,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CoachPalette.CameraDim),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AgentTopStatusBar(state = state)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val readiness = readinessSuffix(state)
                if (readiness.isNotEmpty()) {
                    Text(
                        text = readiness,
                        color = EmergencyPalette.Warn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                AgentFlowRail(agentStage = state.agentStage)
                CoachGuidancePanel(state = state)

                if (state.metronomeRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        MetronomeIndicator(
                            bpm = state.metronomeBpm,
                            running = state.metronomeRunning,
                            inRange = state.rateInRange,
                        )
                    }
                }

                state.primaryButtonLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    BigButton(
                        text = label,
                        container = Color(0xFF16A34A),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPrimaryButton,
                    )
                }

                if (showQuickReplies) {
                    QuickReplyBar(onQuickReply = onQuickReply)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoiceStatusChip(state = state)
                CoachBottomBar(
                    onDialEmergency = onDialEmergency,
                    onStop = onStop,
                )
            }
        }
    }
}

/** 联调快捷短语：模拟用户语音，推进 S1→S7 状态机。 */
@Composable
private fun QuickReplyBar(onQuickReply: (String) -> Unit) {
    val phrases = listOf(
        "现场安全了" to "现场安全",
        "他没有反应" to "没反应",
        "没有正常呼吸" to "没呼吸",
        "开始按压" to "开始按压",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EmergencyPalette.Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "快捷回复（联调）",
            color = EmergencyPalette.OnSurfaceMuted,
            fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            phrases.forEach { (spoken, label) ->
                OutlinedButton(
                    onClick = { onQuickReply(spoken) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BigButton(
    text: String,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
        ),
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

/** 交接报告弹窗。 */
@Composable
private fun HandoverDialog(report: HandoverReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
        title = { Text("交接报告") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(report.headline, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                report.highlights.forEach { Text("• $it", fontSize = 14.sp) }
                report.location?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("定位：%.5f, %.5f".format(it.latitude, it.longitude), fontSize = 13.sp)
                }
            }
        },
    )
}

/** 模型未就绪时的兜底提示，用于“加载慢”场景。 */
private fun readinessSuffix(state: CprSessionState): String = when {
    state.incidentBanner != null -> ""
    !state.agentConnected && state.latestGuidance == null ->
        "Agent 离线 — 请确认 npm run voice:serve；真机/无线需 adb reverse tcp:8787 tcp:8787"
    !state.agentReady -> "指导引擎加载中…"
    !state.liveWsConnected && ServiceLocator.agentBackend == AgentBackend.REMOTE_COPILOT ->
        "Live 语音通道未连接（快捷回复与按钮仍可用）"
    !state.perceptionReady -> "识别启动中…"
    else -> ""
}
