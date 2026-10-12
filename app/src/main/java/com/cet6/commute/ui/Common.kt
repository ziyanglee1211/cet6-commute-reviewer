package com.cet6.commute.ui

import android.content.Context
import android.widget.Toast
import com.cet6.commute.tts.Tts

internal fun fmtMs(ms: Int): String = "%d:%02d".format(ms / 60000, (ms % 60000) / 1000)
internal fun fmtSec(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
internal fun fmtBytes(b: Long): String = if (b >= 1024 * 1024) "%.1f MB".format(b / 1048576.0) else "${b / 1024} KB"

internal fun speakOrToast(ctx: Context, text: String) {
    if (!Tts.speak(text)) {
        Toast.makeText(ctx, "朗读不可用:请在系统设置中为语音引擎下载离线英语语音包", Toast.LENGTH_LONG).show()
    }
}

/** 轻震一下(翻牌/标记反馈),受设置开关控制 */
internal fun haptic(ctx: Context) {
    val app = ctx.applicationContext as? com.cet6.commute.App ?: return
    if (!app.vibrateOnFlip) return
    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
    if (!v.hasVibrator()) return
    v.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
}

/** 根据文本内偏移取所在单词 */
internal fun wordAtOffset(text: String, offset: Int): String {
    if (offset < 0 || offset >= text.length) return ""
    var s = offset
    var e = offset
    while (s > 0 && text[s - 1].isLetter()) s--
    while (e < text.length && text[e].isLetter()) e++
    return text.substring(s, e)
}

/** 取偏移所在完整句子 */
internal fun sentenceAround(text: String, offset: Int): String {
    if (offset < 0 || offset >= text.length) return ""
    var s = 0
    for (i in offset downTo 0) {
        if (text[i] in ".!?\n。!?") { s = i + 1; break }
    }
    var e = text.length
    for (i in offset until text.length) {
        if (text[i] in ".!?\n。!?") { e = i + 1; break }
    }
    return text.substring(s, e).trim()
}
