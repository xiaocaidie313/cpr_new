package com.example.cpr_new.core.contract

/**
 * 指导动作契约（第 2 部分 Gemma Agent -> 第 4 部分）。
 *
 * 设计原则：
 * - Agent 负责“说什么、什么时候说”（医学决策），第 4 部分负责“怎么呈现”（TTS / 大字 / 震动）。
 * - 一个 [GuidanceAction] 同时携带语音文本与屏幕文本，避免二者不一致；
 *   Android 端按设备能力选择性呈现（如静音时只显示大字）。
 * - 通过 [phase] 与 [targetRate] 驱动 UI 状态与节拍器，做到与具体话术解耦。
 */
data class GuidanceAction(
    /** 动作唯一标识，便于日志去重与 UI diff。 */
    val id: String,
    /** 优先级，决定是否打断当前播报与高亮强度。 */
    val priority: GuidancePriority,
    /** 当前急救阶段，驱动 UI 主视图与可用操作。 */
    val phase: CprPhase,
    /** TTS 播报文本（口语化、简短）。为空表示本次不播报。 */
    val spokenText: String = "",
    /** 屏幕大字提示（高对比、动词开头）。 */
    val displayText: String,
    /** 触觉反馈模式，null 表示无震动。 */
    val haptic: HapticPattern? = null,
    /** 节拍器目标频率（次 / 分钟），null 表示沿用当前值 / 不展示节拍器。 */
    val targetRate: Int? = null,
    /** 可扩展元数据，便于 Agent 透传额外提示而不破坏既有字段。 */
    val metadata: Map<String, String> = emptyMap(),
)

/** 指导优先级。CRITICAL 会打断当前 TTS 并强提示。 */
enum class GuidancePriority { INFO, WARNING, CRITICAL }

/**
 * CPR 急救阶段。
 * 对应标准流程：评估 -> 呼叫 120 -> 持续按压 -> 人工呼吸 -> AED -> 交接。
 */
enum class CprPhase {
    /** 评估意识 / 呼吸。 */
    ASSESS,

    /** 呼叫急救（120）。 */
    CALL_EMS,

    /** 胸外按压。 */
    COMPRESSION,

    /** 人工呼吸。 */
    RESCUE_BREATH,

    /** 使用 AED。 */
    AED,

    /** 交接给专业人员（生成 Handover 报告）。 */
    HANDOVER,
}

/** 触觉反馈模式，由硬件层映射为具体震动波形。 */
enum class HapticPattern {
    /** 单次轻触（如节拍）。 */
    TICK,

    /** 双击（提示切换）。 */
    DOUBLE,

    /** 强震（严重告警）。 */
    STRONG,
}
