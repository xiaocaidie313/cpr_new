package com.example.cpr_new.agent.copilot

import android.content.Context
import com.example.cpr_new.BuildConfig
import com.example.cpr_new.core.contract.CprPhase
import com.example.cpr_new.core.contract.GuidanceAction
import com.example.cpr_new.core.contract.GuidanceAgent
import com.example.cpr_new.core.contract.GuidancePriority
import com.example.cpr_new.core.contract.HandoverReport
import com.example.cpr_new.core.contract.PerceptionEvent
import com.example.cpr_new.core.contract.SessionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 通过 HTTP + WebSocket 连接 first-aid-co-pilot Node voice server。
 *
 * - HTTP `/api/turn`：会话启动、按钮回合、感知上报、交接
 * - WS `/ws/live`：流式 guidance、服务端 TTS 音频、阶段状态
 */
class RemoteGuidanceAgent(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val transport: AgentTransport = HttpAgentTransport(BuildConfig.COPILOT_BASE_URL),
    private val wsChannel: WebSocketAgentChannel = WebSocketAgentChannel(BuildConfig.COPILOT_WS_URL),
) : GuidanceAgent, LiveAgentCapable {

    private val turnMutex = Mutex()
    private var sessionId: String = ""
    private var currentStage: String? = null
    private var lastPerceptionTurnMs: Long = 0L
    private var ready: Boolean = false

    override val isReady: Boolean get() = ready

    override val liveEvents: Flow<LiveAgentEvent> = wsChannel.events

    override val isLiveConnected: Boolean get() = wsChannel.isConnected

    override fun connectLive(sessionId: String) {
        this.sessionId = sessionId
        wsChannel.connect(sessionId)
    }

    override fun disconnectLive() {
        wsChannel.close()
    }

    override suspend fun resetSession(sessionId: String) {
        wsChannel.reset()
        transport.reset(sessionId)
        currentStage = null
        lastPerceptionTurnMs = 0L
    }

    override fun commitText(text: String, intent: String?) {
        wsChannel.commitText(text, intent)
    }

    override fun sendTurn(request: TurnRequest) {
        wsChannel.sendTurn(request)
    }

    override fun sendPcm(pcm16: ByteArray) {
        wsChannel.sendPcm(pcm16)
    }

    override fun sendBargeIn() {
        wsChannel.sendBargeIn()
    }

    override suspend fun onSessionStart(sessionId: String): GuidanceAction {
        this.sessionId = sessionId
        connectLive(sessionId)
        ready = transport.health()
        if (!ready) {
            return offlineFallback(
                sessionId,
                "无法连接 Agent 服务。请确认 npm run voice:serve 已启动；" +
                    "真机/无线调试请在终端执行 adb reverse tcp:8787 tcp:8787 后重装 App",
            )
        }

        val deviceState = defaultDeviceState()
        val request = sessionStartedRequest(sessionId, deviceState)
        wsChannel.updateContext(request)
        return postTurn(request) ?: offlineFallback(sessionId, "Agent 未返回启动指导")
    }

    override fun guidance(perception: Flow<PerceptionEvent>): Flow<GuidanceAction> = flow {
        perception.collect { event ->
            if (!event.isReliable()) return@collect
            if (!shouldForwardPerception()) return@collect
            val now = System.currentTimeMillis()
            if (now - lastPerceptionTurnMs < PERCEPTION_MIN_INTERVAL_MS) return@collect
            lastPerceptionTurnMs = now

            val request = PerceptionEventMapper.toCprQualityTurn(
                sessionId = sessionId,
                event = event,
                deviceState = defaultDeviceState(),
            )
            postTurn(request)?.let { emit(it) }
        }
    }

    /**
     * 按钮/快捷回复走 HTTP；在线语音仅 WS commit_text（guidance 由 WS 推送）。
     */
    override suspend fun submitUserTurn(
        sessionId: String,
        text: String,
        viaLiveVoice: Boolean,
    ): GuidanceAction? {
        this.sessionId = sessionId
        if (viaLiveVoice && isLiveConnected) {
            commitText(text)
            return null
        }
        val request = TurnRequest(
            sessionId = sessionId,
            text = text,
            eventSource = "ui",
            eventType = "user_response",
            deviceState = defaultDeviceState(),
        )
        return postTurn(request)
    }

    override suspend fun buildHandover(log: SessionLog): HandoverReport {
        val request = TurnRequest(
            sessionId = log.sessionId,
            eventType = "handover_requested",
            eventSource = "device",
            deviceState = defaultDeviceState(),
        )
        val guidance = postTurn(request)
        val narrative = guidance?.messageText?.takeIf { it.isNotBlank() }
            ?: guidance?.ttsText?.takeIf { it.isNotBlank() }
            ?: "会话 ${log.sessionId.take(8)} 已结束，请结合录音与日志向医护交接。"

        return HandoverReport(
            sessionId = log.sessionId,
            headline = "急救会话交接摘要",
            highlights = listOf(
                "按压时长约 ${log.durationMs?.div(1000) ?: 0} 秒",
                "最终质量分 ${log.metrics["finalQualityScore"]?.toInt() ?: 0}",
                "平均频率 ${log.metrics["avgCompressionRate"]?.toInt() ?: 0} 次/分",
            ),
            narrative = narrative,
            generatedAtMs = System.currentTimeMillis(),
        )
    }

    fun updateStage(stage: String?) {
        currentStage = stage
    }

    private suspend fun postTurn(request: TurnRequest): GuidanceAction? = turnMutex.withLock {
        when (val result = transport.turn(request)) {
            is TurnResult.Success -> {
                val response = result.response
                if (!response.ok) {
                    return offlineFallback(request.sessionId, response.error ?: "Agent 返回错误")
                }
                currentStage = response.currentStage ?: response.guidanceAction?.stage
                response.guidanceAction?.let { copilot ->
                    CopilotActionMapper.toLocalAction(
                        copilot,
                        request.sessionId,
                        ttsAudioSrc = response.ttsAudioSrc,
                    )
                }
            }

            is TurnResult.Failure -> {
                if (isCompressionStage()) {
                    return offlineCompressionFallback(request.sessionId, result.error.message)
                }
                ready = false
                offlineFallback(request.sessionId, result.error.message)
            }
        }
    }

    private fun shouldForwardPerception(): Boolean = isCompressionStage()

    private fun isCompressionStage(): Boolean {
        val stage = currentStage ?: return false
        return stage.contains("S7") || stage.contains("S8") || stage.contains("MONITOR")
    }

    private fun offlineFallback(sessionId: String, message: String): GuidanceAction =
        GuidanceAction(
            actionId = "offline_${System.currentTimeMillis()}",
            sessionId = sessionId,
            messageText = message,
            ttsText = "",
            phase = CprPhase.ASSESS,
            priority = GuidancePriority.HIGH,
            metadata = mapOf("offline" to "true"),
        )

    private fun offlineCompressionFallback(sessionId: String, message: String): GuidanceAction =
        GuidanceAction(
            actionId = "offline_cpr_${System.currentTimeMillis()}",
            sessionId = sessionId,
            messageText = "继续按压",
            ttsText = "",
            phase = CprPhase.COMPRESSION,
            priority = GuidancePriority.HIGH,
            targetRate = 110,
            metadata = mapOf(
                "offline" to "partial",
                "offline_detail" to message,
                "tool_types" to CopilotHapticTools.UPDATE,
                "copilot_stage" to (currentStage ?: "S7_CPR_LOOP"),
            ),
        )

    private fun defaultDeviceState(): Map<String, Any?> =
        mapOf(
            "camera_available" to true,
            "mic_available" to true,
            "gps_available" to true,
            "recording" to true,
            "emergency_call_started" to false,
            "network" to "offline",
        )

    companion object {
        private const val PERCEPTION_MIN_INTERVAL_MS = 2_000L
    }
}
