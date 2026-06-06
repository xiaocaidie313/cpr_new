package com.example.cpr_new.agent.copilot

import org.json.JSONObject

data class TurnResponse(
    val ok: Boolean,
    val sessionId: String?,
    val transcript: String,
    val currentStage: String?,
    val guidanceAction: CopilotGuidanceAction?,
    val guidanceSource: String?,
    val ttsText: String,
    val error: String?,
) {
    val ttsAudioSrc: String? = null
}

fun parseTurnResponse(json: JSONObject): TurnResponse {
    val state = json.optJSONObject("state")
    val guidanceAction = json.optJSONObject("guidance_action")
        ?.takeIf { it.has("action_id") }
        ?.let { runCatching { parseCopilotAction(it) }.getOrNull() }

    val ttsText = guidanceAction?.tts?.text?.takeIf { it.isNotBlank() } ?: ""

    return TurnResponse(
        ok = json.optBoolean("ok", false),
        sessionId = json.stringOrNull("session_id"),
        transcript = json.optString("transcript", ""),
        currentStage = state?.stringOrNull("current_stage"),
        guidanceAction = guidanceAction,
        guidanceSource = json.stringOrNull("guidance_source"),
        ttsText = ttsText,
        error = json.optJSONObject("error")?.stringOrNull("message"),
    )
}

private fun JSONObject.stringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null
