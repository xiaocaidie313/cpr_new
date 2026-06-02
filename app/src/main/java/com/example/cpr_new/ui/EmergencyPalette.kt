package com.example.cpr_new.ui

import androidx.compose.ui.graphics.Color

/**
 * 急救场景专用高对比配色 —— 急救 UI。
 *
 * 急救场景下用户处于高压、强光/暗光、可能戴手套，故：
 * - 背景深色、文字纯白，最大化对比度；
 * - 用红/黄/绿三态强语义色表达“危险/警告/达标”，符合直觉、无需阅读。
 * 这些颜色独立于 Material 主题色，确保在任何系统主题下都稳定可读。
 */
object EmergencyPalette {
    val Background = Color(0xFF0E0F12)

    /** 背景渐变端点：顶部略带暖红，暗示“急救”语境但不刺眼。 */
    val BackgroundTop = Color(0xFF15100F)
    val BackgroundBottom = Color(0xFF0B0C0F)

    val Surface = Color(0xFF1A1C20)

    /** 比 Surface 略亮，用于卡片内的指标小块，制造轻微层次。 */
    val SurfaceVariant = Color(0xFF24272D)

    /** 卡片描边：低调勾勒边界，避免暗色面块糊在一起。 */
    val Outline = Color(0xFF2E323A)

    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceMuted = Color(0xFFB7BCC4)

    /** 达标 / 正确。 */
    val Good = Color(0xFF23C552)

    /** 警告 / 需要调整。 */
    val Warn = Color(0xFFFFB020)

    /** 危险 / 严重告警。 */
    val Danger = Color(0xFFFF3B30)

    /** 主操作（开始急救）。 */
    val Primary = Color(0xFFE53935)

    /** 中性强调（节拍器脉冲）。 */
    val Accent = Color(0xFF3DA9FC)

    /** 语义色的柔和底色（半透明），用于指标块/告警的背景填充。 */
    val GoodSoft = Color(0x2623C552)
    val WarnSoft = Color(0x26FFB020)
    val DangerSoft = Color(0x26FF3B30)
    val AccentSoft = Color(0x263DA9FC)

    /** 未达标 / 未点亮的轨道颜色（步进条、环形进度的底环）。 */
    val Track = Color(0xFF34383F)
}
