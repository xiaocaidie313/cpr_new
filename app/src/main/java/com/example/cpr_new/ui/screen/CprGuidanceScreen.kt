package com.example.cpr_new.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpr_new.BuildConfig
import com.example.cpr_new.core.contract.FrameSink
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.feature.session.CprSessionState
import com.example.cpr_new.hardware.camera.CameraPreview
import com.example.cpr_new.ui.AttentionMode
import com.example.cpr_new.ui.AttentionModeInputs
import com.example.cpr_new.ui.CoachPalette
import com.example.cpr_new.ui.coachContentPadding
import com.example.cpr_new.ui.component.AgentTopStatusBar
import com.example.cpr_new.ui.component.CoachAttentionLayout
import com.example.cpr_new.ui.component.CprCoachOverlay
import com.example.cpr_new.ui.component.EyesOffAttentionLayout
import com.example.cpr_new.ui.component.GlanceableAttentionLayout
import com.example.cpr_new.ui.component.IdleHeroVisual
import com.example.cpr_new.ui.component.IdleStartBar
import com.example.cpr_new.ui.component.LiveVoiceControls
import com.example.cpr_new.ui.component.StatusBanner
import com.example.cpr_new.ui.toAttentionMode

/**
 * 急救指导主界面。
 *
 * 进行态层叠结构对齐 first-aid-co-pilot [LiveCprCoachScreen]：
 * 相机 → Canvas 叠层 → 注意力布局 → 顶栏 → 底栏语音控制。
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
    onStartAudio: () -> Unit = {},
    onStopAudio: () -> Unit = {},
    onSubmitText: (String) -> Unit = {},
    onRequestMicPermission: () -> Unit = {},
    onConfirmPendingTool: (Boolean) -> Unit = {},
    showQuickReplies: Boolean = false,
    hasMicPermission: Boolean = false,
    modifier: Modifier = Modifier,
    cameraGranted: Boolean = false,
    frameSink: FrameSink? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CoachPalette.Background),
    ) {
        if (!state.isActive) {
            IdleContent(state = state, onStart = onStart, onDialEmergency = onDialEmergency)
        } else {
            ActiveContent(
                state = state,
                onStop = onStop,
                onDialEmergency = onDialEmergency,
                onPrimaryButton = onPrimaryButton,
                onQuickReply = onQuickReply,
                onStartAudio = onStartAudio,
                onStopAudio = onStopAudio,
                onSubmitText = onSubmitText,
                onRequestMicPermission = onRequestMicPermission,
                showQuickReplies = showQuickReplies,
                hasMicPermission = hasMicPermission,
                cameraGranted = cameraGranted,
                frameSink = frameSink,
            )
        }

        if (state.isActive && state.incidentBanner != null) {
            StatusBanner(
                incident = state.incidentBanner,
                onDismissIncident = onDismissIncident,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    .zIndex(12f),
            )
        }

        state.handoverReport?.let { report ->
            HandoverDialog(report = report, onDismiss = onDismissReport)
        }

        if (state.pendingConfirmTool != null) {
            PendingConfirmDialog(
                message = state.pendingConfirmMessage.orEmpty(),
                onConfirm = { onConfirmPendingTool(true) },
                onDismiss = { onConfirmPendingTool(false) },
            )
        }
    }
}

/** 待命态：与进行态同一深色视觉语言，底部单条操作栏。 */
@Composable
private fun IdleContent(
    state: CprSessionState,
    onStart: () -> Unit,
    onDialEmergency: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))

        Box(
            modifier = Modifier
                .border(2.dp, CoachPalette.CameraBorder, RoundedCornerShape(120.dp))
                .clip(RoundedCornerShape(120.dp)),
        ) {
            IdleHeroVisual()
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "FirstAid Copilot",
            color = CoachPalette.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "对准胸部，语音或按钮即可获得实时 CPR 指导",
            color = CoachPalette.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
        )

        Spacer(Modifier.height(24.dp))
        IdleStepsCard()

        if (state.handoverReport != null) {
            Text(
                text = "上次急救报告已生成",
                color = CoachPalette.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "开始后将请求相机、麦克风与定位权限",
            color = CoachPalette.TextHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        IdleStartBar(onStart = onStart, onDialEmergency = onDialEmergency)
    }
}

@Composable
private fun IdleStepsCard() {
    Surface(
        color = CoachPalette.Panel,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IdleStep("1", "确认环境安全，轻拍呼叫患者")
            IdleStep("2", "无反应无呼吸 → 立即拨打 120")
            IdleStep("3", "跟随节拍，用力快速按压胸部")
        }
    }
}

@Composable
private fun IdleStep(index: String, text: String) {
    Text(
        text = "$index  $text",
        color = CoachPalette.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

/** 进行态：严格对齐 LiveCprCoachScreen 的 Box 层叠。 */
@Composable
private fun ActiveContent(
    state: CprSessionState,
    onStop: () -> Unit,
    onDialEmergency: () -> Unit,
    onPrimaryButton: () -> Unit,
    onQuickReply: (String) -> Unit,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onSubmitText: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
    showQuickReplies: Boolean,
    hasMicPermission: Boolean,
    cameraGranted: Boolean,
    frameSink: FrameSink?,
) {
    val attentionMode = AttentionModeInputs(
        agentStage = state.agentStage,
        visualOverlayMode = state.visualOverlayMode,
    ).toAttentionMode()
    var inputExpanded by remember { mutableStateOf(false) }
    val contentPadding = coachContentPadding(inputExpanded)

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            enabled = cameraGranted,
            sessionId = state.sessionId,
            frameSink = frameSink,
            modifier = Modifier.fillMaxSize(),
        )

        CprCoachOverlay(
            mode = state.visualOverlayMode,
            correctionArrow = state.correctionArrow,
            attentionMode = attentionMode,
            modifier = Modifier.fillMaxSize(),
        )

        when (attentionMode) {
            AttentionMode.Coach -> CoachAttentionLayout(
                state = state,
                contentPadding = contentPadding,
                onPrimaryButton = onPrimaryButton,
            )
            AttentionMode.EyesOff -> EyesOffAttentionLayout(
                state = state,
                contentPadding = contentPadding,
            )
            AttentionMode.Glanceable -> GlanceableAttentionLayout(
                state = state,
                contentPadding = contentPadding,
                onPrimaryButton = onPrimaryButton,
            )
        }

        AgentTopStatusBar(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(10f),
        )

        LiveVoiceControls(
            state = state,
            onStartAudio = onStartAudio,
            onStopAudio = onStopAudio,
            onSubmitText = onSubmitText,
            onRequestMicPermission = onRequestMicPermission,
            onDialEmergency = onDialEmergency,
            onStop = onStop,
            hasMicPermission = hasMicPermission,
            onInputExpandedChanged = { inputExpanded = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(10f),
        )

        if (showQuickReplies && BuildConfig.DEBUG) {
            DebugQuickReplyBar(
                onQuickReply = onQuickReply,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .zIndex(5f),
            )
        }
    }
}

@Composable
private fun DebugQuickReplyBar(onQuickReply: (String) -> Unit, modifier: Modifier = Modifier) {
    val phrases = listOf("现场安全了", "他没有反应", "没有正常呼吸", "开始按压")
    Surface(
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            phrases.forEach { phrase ->
                Text(
                    text = phrase,
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onQuickReply(phrase) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PendingConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CoachPalette.CameraFrame,
        titleContentColor = CoachPalette.TextPrimary,
        textContentColor = CoachPalette.TextSecondary,
        title = { Text("需要确认", fontWeight = FontWeight.Black) },
        text = {
            Text(
                text = message.ifBlank { "Agent 请求执行一项需要授权的操作，是否继续？" },
                fontSize = 15.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CoachPalette.TextMuted)
            }
        },
    )
}

@Composable
private fun HandoverDialog(report: HandoverReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CoachPalette.CameraFrame,
        titleContentColor = CoachPalette.TextPrimary,
        textContentColor = CoachPalette.TextSecondary,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
            }
        },
        title = { Text("交接报告", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(report.headline, color = CoachPalette.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                report.highlights.forEach { Text("• $it", fontSize = 14.sp) }
                report.location?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("定位：%.5f, %.5f".format(it.latitude, it.longitude), color = Color(0xFF93C5FD))
                }
            }
        },
    )
}
