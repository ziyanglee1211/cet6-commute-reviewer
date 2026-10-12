package com.cet6.commute.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 系统 TTS 封装(应用自身零网络请求;朗读质量取决于系统离线语音包) */
object Tts {
    private var tts: TextToSpeech? = null
    private var initOk = false

    /** true=英文朗读可用;false=系统缺少英文语音数据 */
    var missingEnglish: Boolean = false
        private set

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                initOk = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                missingEnglish = !initOk
            } else {
                missingEnglish = true
            }
        }
    }

    /** @return 是否成功提交朗读 */
    fun speak(text: String): Boolean {
        val t = tts ?: return false
        if (!initOk) return false
        return try {
            t.setSpeechRate(0.9f)
            val r = t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cet6_${System.currentTimeMillis()}")
            r == TextToSpeech.SUCCESS
        } catch (_: Exception) {
            false
        }
    }
}
