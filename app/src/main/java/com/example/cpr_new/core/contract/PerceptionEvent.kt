package com.example.cpr_new.core.contract

/**
 * 感知事件契约（第 3 部分 -> 第 4 部分）。
 *
 * 设计原则：
 * - 这是“CPR 感知识别 / 质量评估”模块向 Android 终端输出的**结构化结果**，
 *   第 4 部分只消费、不计算。算法本身由第 3 部分维护。
 * - 字段全部可空 / 带置信度，便于在识别失败、模型未就绪时由 UI 做兜底降级。
 * - [rawSignals] 作为可扩展口袋字段，后续第 3 部分新增原始信号时无需改动 Android 端编译。
 *
 * 对应分工表第 3 部分输出：hand_position / compression_rate / interruption / confidence 等。
 */
data class PerceptionEvent(
    /** 事件产生时间（毫秒，System.currentTimeMillis 或单调时钟，由生产者约定）。 */
    val timestampMs: Long,
    /** 手部按压位置评估。 */
    val handPosition: HandPosition,
    /** 按压频率（次 / 分钟）。null 表示当前帧无法判定。 */
    val compressionRate: Float? = null,
    /** 按压深度充分性评分（0f~1f），越接近 1 表示越达标。null 表示无法判定。 */
    val compressionDepthScore: Float? = null,
    /** 是否检测到按压中断（停顿过久）。 */
    val isInterrupted: Boolean = false,
    /** 本次识别整体置信度（0f~1f）。低置信度时 UI 应弱化提示、避免误导。 */
    val confidence: Float = 0f,
    /** 可扩展原始信号字典（如关键点坐标、节拍能量等），默认空，便于平滑演进。 */
    val rawSignals: Map<String, Float> = emptyMap(),
) {
    /** 置信度是否达到可用于驱动 UI 的最低阈值。集中在此便于统一调参。 */
    fun isReliable(threshold: Float = MIN_RELIABLE_CONFIDENCE): Boolean = confidence >= threshold

    companion object {
        const val MIN_RELIABLE_CONFIDENCE = 0.5f

        /** 标准成人 CPR 推荐按压频率区间（次 / 分钟）。 */
        const val TARGET_RATE_MIN = 100
        const val TARGET_RATE_MAX = 120
    }
}

/** 手部按压位置的离散评估结果。 */
enum class HandPosition {
    /** 位置正确。 */
    CORRECT,

    /** 偏高（偏向胸骨上段）。 */
    TOO_HIGH,

    /** 偏低（偏向剑突）。 */
    TOO_LOW,

    /** 左右偏离中线。 */
    OFF_CENTER,

    /** 无法识别 / 画面中无有效目标。 */
    UNKNOWN,
}
