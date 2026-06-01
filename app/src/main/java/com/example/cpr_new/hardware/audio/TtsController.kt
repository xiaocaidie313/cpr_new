package com.example.cpr_new.hardware.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 文本转语音（TTS）控制器 —— 硬件适配层。
 *
 * 职责：把 Agent 的 [com.example.cpr_new.core.contract.GuidanceAction.spokenText] 念出来。
 * 设计要点：
 * - 懒初始化 + 就绪标志，避免引擎未就绪时丢播报；
 * - CRITICAL 级别可打断当前播报（QUEUE_FLUSH），普通提示排队（QUEUE_ADD）；
 * - 全程容错：引擎初始化失败 / 语言不支持时静默降级，由 UI 大字兜底，不崩溃。
 *
 * 注意：本类不感知业务，只提供“说话”能力，便于复用与替换（如换成云端 TTS）。
 */
class TtsController(context: Context) {

    private val ready = AtomicBoolean(false)
    private var initFailed = false

    // 用 applicationContext 防止 Activity 泄漏。
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = engineSetLanguage()
            ready.set(result)
            initFailed = !result
        } else {
            initFailed = true
        }
    }

    /** 设置中文优先、英文兜底。返回是否成功设置了可用语言。 */
    private fun engineSetLanguage(): Boolean {
        val zh = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (zh != TextToSpeech.LANG_MISSING_DATA && zh != TextToSpeech.LANG_NOT_SUPPORTED) {
            return true
        }
        val en = tts.setLanguage(Locale.US)
        return en != TextToSpeech.LANG_MISSING_DATA && en != TextToSpeech.LANG_NOT_SUPPORTED
    }

    val isReady: Boolean get() = ready.get()
    val isUnavailable: Boolean get() = initFailed

    /**
     * 播报文本。
     * @param text 要播报内容；空字符串直接忽略。
     * @param interrupt 是否打断当前播报（用于高优先级提示）。
     */
    fun speak(text: String, interrupt: Boolean = false) {
        if (text.isBlank() || !ready.get()) return
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        // utteranceId 便于将来做播报完成回调 / 埋点。
        tts.speak(text, mode, null, "cpr-${System.nanoTime()}")
    }

    /** 立即停止当前播报队列。 */
    fun stop() {
        if (ready.get()) tts.stop()
    }

    /** 可选：注册播报进度回调（如播报中暂停节拍器语音冲突）。 */
    fun setProgressListener(listener: UtteranceProgressListener) {
        tts.setOnUtteranceProgressListener(listener)
    }

    /** 释放引擎，务必在宿主销毁时调用。 */
    fun release() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
        ready.set(false)
    }
}
