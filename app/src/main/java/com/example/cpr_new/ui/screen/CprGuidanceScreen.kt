package com.example.cpr_new.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpr_new.core.contract.CprPhase
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.feature.session.CprSessionState
import com.example.cpr_new.ui.EmergencyPalette
import com.example.cpr_new.ui.component.GuidanceCard
import com.example.cpr_new.ui.component.MetronomeIndicator
import com.example.cpr_new.ui.component.QualityDashboard
import com.example.cpr_new.ui.component.StatusBanner

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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EmergencyPalette.Background)
            .padding(16.dp),
    ) {
        if (!state.isActive) {
            IdleContent(state = state, onStart = onStart)
        } else {
            ActiveContent(
                state = state,
                onStop = onStop,
                onDialEmergency = onDialEmergency,
                onDismissIncident = onDismissIncident,
            )
        }

        // 会话结束后弹出交接报告。
        state.handoverReport?.let { report ->
            HandoverDialog(report = report, onDismiss = onDismissReport)
        }
    }
}

/** 待命态：大号开始按钮 + 简要说明。 */
@Composable
private fun IdleContent(state: CprSessionState, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "FirstAid Copilot",
            color = EmergencyPalette.OnSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "对准胸部，开始即可获得实时 CPR 指导",
            color = EmergencyPalette.OnSurfaceMuted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
        )
        BigButton(
            text = "开始急救",
            container = EmergencyPalette.Primary,
            onClick = onStart,
        )
        // 上一轮报告未关时也能在这里看到入口提示。
        if (state.handoverReport != null) {
            Text(
                text = "上次急救报告已生成",
                color = EmergencyPalette.OnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** 进行态：横幅 + 大字指导 + 仪表盘 + 节拍器 + 操作区。 */
@Composable
private fun ActiveContent(
    state: CprSessionState,
    onStop: () -> Unit,
    onDialEmergency: () -> Unit,
    onDismissIncident: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBanner(
            phaseLabel = phaseLabel(state.phase) + readinessSuffix(state),
            incident = state.incidentBanner,
            onDismissIncident = onDismissIncident,
        )
        Spacer(Modifier.height(12.dp))

        GuidanceCard(displayText = state.latestGuidance?.displayText.orEmpty())
        Spacer(Modifier.height(16.dp))

        QualityDashboard(
            qualityScore = state.qualityScore,
            perception = state.latestPerception,
            rateInRange = state.rateInRange,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            MetronomeIndicator(bpm = state.metronomeBpm, running = state.metronomeRunning)
        }

        Spacer(Modifier.weight(1f))

        // 底部操作区：醒目的 120 与结束按钮。
        Row(modifier = Modifier.fillMaxWidth()) {
            BigButton(
                text = "拨打 120",
                container = EmergencyPalette.Danger,
                modifier = Modifier.weight(1f),
                onClick = onDialEmergency,
            )
            Spacer(Modifier.width(12.dp))
            BigButton(
                text = "结束",
                container = EmergencyPalette.Surface,
                modifier = Modifier.weight(1f),
                onClick = onStop,
            )
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

private fun phaseLabel(phase: CprPhase): String = when (phase) {
    CprPhase.ASSESS -> "阶段：评估意识与呼吸"
    CprPhase.CALL_EMS -> "阶段：呼叫急救 120"
    CprPhase.COMPRESSION -> "阶段：胸外按压"
    CprPhase.RESCUE_BREATH -> "阶段：人工呼吸"
    CprPhase.AED -> "阶段：使用 AED"
    CprPhase.HANDOVER -> "阶段：交接"
}

/** 模型未就绪时在阶段标签后追加提示，用于“加载慢”兜底。 */
private fun readinessSuffix(state: CprSessionState): String = when {
    !state.agentReady -> "（指导引擎加载中…）"
    !state.perceptionReady -> "（识别启动中…）"
    else -> ""
}
