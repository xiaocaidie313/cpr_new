package com.example.cpr_new.feature.session

import com.example.cpr_new.core.contract.HandPosition
import com.example.cpr_new.core.contract.PerceptionEvent
import kotlin.math.abs

/**
 * 按压质量评分器 —— 把感知指标聚合成 0~100 的直观分数。
 *
 * 重要边界：这里只是**UI 展示用的轻量加权**，不是医学评分标准；
 * 真正的质量评估算法属于第 3 部分。把它独立成纯函数，方便单测与调权重。
 */
object QualityScorer {

    /**
     * 计算综合质量分。
     * 权重：频率 40% + 深度 35% + 手位 25%；中断时整体打折。
     */
    fun score(event: PerceptionEvent): Int {
        // 低置信度时不夸大分数，按置信度线性衰减。
        val reliability = event.confidence.coerceIn(0f, 1f)

        val rateScore = event.compressionRate?.let { rateScore(it) } ?: 0f
        val depthScore = (event.compressionDepthScore ?: 0f).coerceIn(0f, 1f)
        val handScore = handScore(event.handPosition)

        var combined = rateScore * 0.40f + depthScore * 0.35f + handScore * 0.25f
        if (event.isInterrupted) combined *= 0.6f // 中断按压显著扣分
        combined *= reliability

        return (combined * 100).toInt().coerceIn(0, 100)
    }

    /** 频率越接近区间中值越高，超出 [TARGET_RATE_MIN, TARGET_RATE_MAX] 线性衰减。 */
    private fun rateScore(rate: Float): Float {
        val mid = (PerceptionEvent.TARGET_RATE_MIN + PerceptionEvent.TARGET_RATE_MAX) / 2f
        val halfRange = (PerceptionEvent.TARGET_RATE_MAX - PerceptionEvent.TARGET_RATE_MIN) / 2f
        val distance = abs(rate - mid)
        // 在区间内得分 0.7~1.0，超出后快速衰减。
        return when {
            distance <= halfRange -> 1f - 0.3f * (distance / halfRange)
            else -> (0.7f - 0.05f * (distance - halfRange)).coerceAtLeast(0f)
        }
    }

    private fun handScore(position: HandPosition): Float = when (position) {
        HandPosition.CORRECT -> 1f
        HandPosition.OFF_CENTER -> 0.5f
        HandPosition.TOO_HIGH, HandPosition.TOO_LOW -> 0.4f
        HandPosition.UNKNOWN -> 0f
    }
}
