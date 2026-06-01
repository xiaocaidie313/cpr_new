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
    val Surface = Color(0xFF1A1C20)
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
}
