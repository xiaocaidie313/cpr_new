package com.example.cpr_new.mock

import com.example.cpr_new.core.contract.CprPerceptionSource
import com.example.cpr_new.core.contract.HandPosition
import com.example.cpr_new.core.contract.PerceptionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * 感知源的 Mock 实现 —— 让第 4 部分在没有第 3 部分模型时也能端到端演示。
 *
 * 行为：模拟一段“逐渐稳定”的按压过程：
 * - 起步频率偏快/手位不准，随时间收敛到标准区间与正确手位；
 * - 周期性插入一次“中断”和一次“低置信度”帧，用于演示 UI 兜底。
 *
 * 真实实现替换后本类可删除，不影响其它代码（依赖的是接口而非本类）。
 */
class MockCprPerceptionSource(
    private val emitIntervalMs: Long = 500,
) : CprPerceptionSource {

    @Volatile
    private var running = false
    override val isReady: Boolean get() = true

    override val events: Flow<PerceptionEvent> = flow {
        var tick = 0
        while (running) {
            emit(fakeEvent(tick))
            tick++
            delay(emitIntervalMs)
        }
    }

    override fun start() { running = true }
    override fun stop() { running = false }

    /** 生成一帧带噪声、随时间收敛的假数据。 */
    private fun fakeEvent(tick: Int): PerceptionEvent {
        // 频率从 ~135 收敛到 ~110。
        val baseRate = 135f - (tick * 1.5f).coerceAtMost(28f)
        val rate = baseRate + Random.nextFloat() * 6f - 3f

        val lowConfidenceFrame = tick % 13 == 7
        val interruptedFrame = tick % 17 == 5

        val handPosition = when {
            lowConfidenceFrame -> HandPosition.UNKNOWN
            tick < 6 -> HandPosition.TOO_HIGH
            tick < 12 -> HandPosition.OFF_CENTER
            else -> HandPosition.CORRECT
        }

        return PerceptionEvent(
            timestampMs = System.currentTimeMillis(),
            handPosition = handPosition,
            compressionRate = rate,
            compressionDepthScore = (0.6f + tick * 0.02f).coerceAtMost(0.95f),
            isInterrupted = interruptedFrame,
            confidence = if (lowConfidenceFrame) 0.3f else 0.85f + Random.nextFloat() * 0.1f,
            rawSignals = mapOf("mockTick" to tick.toFloat()),
        )
    }
}
