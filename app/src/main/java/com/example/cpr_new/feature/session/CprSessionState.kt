package com.example.cpr_new.feature.session

import com.example.cpr_new.core.contract.CprPhase
import com.example.cpr_new.core.contract.GuidanceAction
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.core.contract.HandPosition
import com.example.cpr_new.core.contract.PerceptionEvent

/**
 * 急救会话 UI 状态 —— 单一数据源（Single Source of Truth）。
 *
 * ViewModel 持有并更新它，Compose UI 只读渲染。所有字段都带默认值，
 * 保证“未开始 / 降级 / 异常”时 UI 都有可渲染的安全状态。
 */
data class CprSessionState(
    /** 会话是否进行中。 */
    val isActive: Boolean = false,
    /** 当前会话 id（驱动摄像头帧 / 日志对齐）。空串表示未开始。 */
    val sessionId: String = "",
    /** 当前急救阶段。 */
    val phase: CprPhase = CprPhase.ASSESS,

    /** 最近一次感知结果（可能为 null：尚未开始 / 识别失败）。 */
    val latestPerception: PerceptionEvent? = null,
    /** 最近一条 Agent 指导。 */
    val latestGuidance: GuidanceAction? = null,

    /** 实时按压质量分（0~100），由感知指标聚合而来，供仪表盘展示。 */
    val qualityScore: Int = 0,
    /** 节拍器目标频率与运行状态（镜像自 Metronome，方便 UI 一处读取）。 */
    val metronomeBpm: Int = Metronome.DEFAULT_BPM,
    val metronomeRunning: Boolean = false,

    /** 就绪标志：用于“模型加载中”兜底提示。 */
    val perceptionReady: Boolean = false,
    val agentReady: Boolean = false,

    /** 当前置顶的兜底/告警提示（断网、权限、识别失败等），null 表示无。 */
    val incidentBanner: String? = null,

    /** 会话结束后的交接报告。 */
    val handoverReport: HandoverReport? = null,
) {
    /** 频率是否在标准区间内，UI 用于决定仪表盘配色。 */
    val rateInRange: Boolean
        get() = latestPerception?.compressionRateBpm?.let {
            it >= PerceptionEvent.TARGET_RATE_MIN && it <= PerceptionEvent.TARGET_RATE_MAX
        } ?: false

    /** 手位是否正确（用于 UI 高亮）。 */
    val handPositionOk: Boolean
        get() = latestPerception?.handPosition == HandPosition.CENTER
}
