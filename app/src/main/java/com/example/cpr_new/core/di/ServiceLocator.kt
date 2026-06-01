package com.example.cpr_new.core.di

import android.content.Context
import com.example.cpr_new.core.contract.CprPerceptionSource
import com.example.cpr_new.core.contract.GuidanceAgent
import com.example.cpr_new.feature.emergency.EmergencyDialer
import com.example.cpr_new.hardware.audio.AudioRecorderController
import com.example.cpr_new.hardware.audio.TtsController
import com.example.cpr_new.hardware.haptics.HapticController
import com.example.cpr_new.hardware.location.LocationProvider
import com.example.cpr_new.mock.MockCprPerceptionSource
import com.example.cpr_new.mock.MockGuidanceAgent

/**
 * 极简服务定位器（手写依赖注入）。
 *
 * 为什么不直接上 Hilt/Koin：MVP 阶段保持零额外依赖、零编译期开销，
 * 同时仍满足“可替换实现”的核心诉求。后续团队需要时可平滑迁移到 Hilt。
 *
 * === 第 2 / 3 部分对接点（重要）===
 * 当真实实现就绪时，只改这里两行：
 *   perceptionFactory = { ctx -> RealPerceptionSource(ctx) }   // 第 3 部分
 *   agentFactory      = { ctx -> GemmaGuidanceAgent(ctx) }     // 第 2 部分
 * 其余 UI / 编排代码完全不动。
 */
object ServiceLocator {

    /** 感知源工厂。默认 Mock，可被替换为第 3 部分真实实现。 */
    var perceptionFactory: (Context) -> CprPerceptionSource = { MockCprPerceptionSource() }

    /** Agent 工厂。默认 Mock，可被替换为第 2 部分真实实现。 */
    var agentFactory: (Context) -> GuidanceAgent = { MockGuidanceAgent() }

    /**
     * 按需组装一份依赖。每次会话创建独立实例，避免跨会话状态串台。
     * 硬件控制器以 applicationContext 构建，安全且无内存泄漏。
     */
    fun provideDependencies(context: Context): CprDependencies {
        val app = context.applicationContext
        return CprDependencies(
            perceptionSource = perceptionFactory(app),
            agent = agentFactory(app),
            tts = TtsController(app),
            haptics = HapticController(app),
            location = LocationProvider(app),
            recorder = AudioRecorderController(app),
            dialer = EmergencyDialer(app),
        )
    }
}
