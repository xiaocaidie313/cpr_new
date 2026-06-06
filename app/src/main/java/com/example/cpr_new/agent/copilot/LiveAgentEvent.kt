package com.example.cpr_new.agent.copilot

import kotlinx.coroutines.flow.Flow

/** 支持 `/ws/live` 全双工通道的 Agent 能力。 */
interface LiveAgentCapable {
    val liveEvents: Flow<LiveAgentEvent>
    fun connectLive(sessionId: String)
    fun disconnectLive()
    fun commitText(text: String, intent: String? = null)
}

sealed interface LiveAgentEvent {
    data class ConnectionChanged(val connected: Boolean, val message: String? = null) : LiveAgentEvent

    data class Guidance(
        val action: CopilotGuidanceAction,
        val currentStage: String? = null,
        val guidanceSource: String? = null,
    ) : LiveAgentEvent

    data class State(val currentStage: String?) : LiveAgentEvent

    data class PartialTranscript(val text: String) : LiveAgentEvent

    data class FinalTranscript(val text: String, val intent: String?) : LiveAgentEvent

    data class AudioBegin(
        val actionId: String?,
        val sampleRate: Int,
        val flushQueue: Boolean,
    ) : LiveAgentEvent

    data class AudioChunk(val bytes: ByteArray) : LiveAgentEvent {
        override fun equals(other: Any?): Boolean =
            other is AudioChunk && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class AudioEnd(val actionId: String?) : LiveAgentEvent

    data class AudioCancel(val reason: String?) : LiveAgentEvent

    data class Error(val message: String) : LiveAgentEvent
}
