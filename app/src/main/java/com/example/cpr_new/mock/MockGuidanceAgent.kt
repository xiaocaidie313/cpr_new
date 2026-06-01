package com.example.cpr_new.mock

import com.example.cpr_new.core.contract.CprPhase
import com.example.cpr_new.core.contract.GuidanceAction
import com.example.cpr_new.core.contract.GuidanceAgent
import com.example.cpr_new.core.contract.GuidancePriority
import com.example.cpr_new.core.contract.HandPosition
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.core.contract.HapticPattern
import com.example.cpr_new.core.contract.PerceptionEvent
import com.example.cpr_new.core.contract.SessionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Agent 的 Mock 实现 —— 用一套**规则**模拟 Gemma 的指导输出。
 *
 * 注意：这里的“规则”仅为演示连通性，**不代表医学标准**；
 * 真实医学决策由第 2 部分 Gemma 负责。替换后本类可删除。
 *
 * 它根据感知事件给出对应纠正话术：手位不准、频率过快/过慢、按压中断等。
 */
class MockGuidanceAgent : GuidanceAgent {

    override val isReady: Boolean get() = true

    override suspend fun onSessionStart(): GuidanceAction = GuidanceAction(
        id = "start",
        priority = GuidancePriority.CRITICAL,
        phase = CprPhase.COMPRESSION,
        spokenText = "开始按压，双手交叠置于胸部中央，用力快速按压。",
        displayText = "用力按压\n胸部中央",
        haptic = HapticPattern.DOUBLE,
        targetRate = 110,
    )

    override fun guidance(perception: Flow<PerceptionEvent>): Flow<GuidanceAction> = flow {
        var lastDisplay = ""
        perception.collect { event ->
            val action = decide(event)
            // 去抖：相同提示不重复播报，避免刷屏与 TTS 拥塞。
            if (action != null && action.displayText != lastDisplay) {
                lastDisplay = action.displayText
                emit(action)
            }
        }
    }

    /** 极简规则引擎：按优先级返回一条最该提醒的动作。 */
    private fun decide(event: PerceptionEvent): GuidanceAction? {
        if (event.isInterrupted) {
            return GuidanceAction(
                id = "interrupt",
                priority = GuidancePriority.CRITICAL,
                phase = CprPhase.COMPRESSION,
                spokenText = "不要停，继续按压！",
                displayText = "不要停顿\n继续按压",
                haptic = HapticPattern.STRONG,
                targetRate = 110,
            )
        }
        when (event.handPosition) {
            HandPosition.TOO_HIGH, HandPosition.TOO_LOW, HandPosition.OFF_CENTER ->
                return GuidanceAction(
                    id = "hand_${event.handPosition}",
                    priority = GuidancePriority.WARNING,
                    phase = CprPhase.COMPRESSION,
                    spokenText = "调整手的位置到胸部正中。",
                    displayText = "手位偏移\n移到胸部中央",
                    haptic = HapticPattern.DOUBLE,
                )
            else -> Unit
        }
        val rate = event.compressionRate
        if (rate != null) {
            if (rate > PerceptionEvent.TARGET_RATE_MAX) {
                return GuidanceAction(
                    id = "rate_fast",
                    priority = GuidancePriority.WARNING,
                    phase = CprPhase.COMPRESSION,
                    spokenText = "节奏太快，跟着节拍稍微放慢。",
                    displayText = "稍慢一点\n跟随节拍",
                    targetRate = 110,
                )
            }
            if (rate < PerceptionEvent.TARGET_RATE_MIN) {
                return GuidanceAction(
                    id = "rate_slow",
                    priority = GuidancePriority.WARNING,
                    phase = CprPhase.COMPRESSION,
                    spokenText = "再快一点，加快按压频率。",
                    displayText = "再快一点\n加快按压",
                    targetRate = 110,
                )
            }
        }
        // 一切正常：给正反馈。
        return GuidanceAction(
            id = "good",
            priority = GuidancePriority.INFO,
            phase = CprPhase.COMPRESSION,
            spokenText = "很好，保持这个节奏。",
            displayText = "做得好\n保持节奏",
        )
    }

    override suspend fun buildHandover(log: SessionLog): HandoverReport {
        val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val avgRate = log.metrics["avgCompressionRate"]?.toInt() ?: 0
        val quality = log.metrics["finalQualityScore"]?.toInt() ?: 0
        val durationSec = (log.durationMs ?: 0L) / 1000

        return HandoverReport(
            sessionId = log.sessionId,
            headline = "施救约 ${durationSec}s，平均频率 $avgRate 次/分，质量分 $quality。",
            highlights = listOf(
                "开始时间：${df.format(Date(log.startedAtMs))}",
                "持续时长：${durationSec} 秒",
                "平均按压频率：$avgRate 次/分",
                "综合质量评分：$quality / 100",
                "事件条目：${log.entries.size} 条",
            ),
            narrative = buildString {
                appendLine("【旁观者 CPR 交接报告（演示）】")
                appendLine("持续约 ${durationSec} 秒，平均按压频率 $avgRate 次/分，综合质量 $quality 分。")
                appendLine("过程中系统已实时纠正手位与节奏，并保存现场录音备查。")
                appendLine("注：本报告由演示用 Mock Agent 生成，不构成医学结论。")
            },
            generatedAtMs = System.currentTimeMillis(),
        )
    }
}
