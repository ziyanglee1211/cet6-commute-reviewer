package com.cet6.commute.logic

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 阶段切换提示音 + 震动 */
object Alert {

    private var tone: ToneGenerator? = null

    private fun vibrator(ctx: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun beep(toneId: Int, durationMs: Int) {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tone?.startTone(toneId, durationMs)
        } catch (_: Exception) {
        }
    }

    private fun vibrate(ctx: Context, ms: Long) {
        try {
            vibrator(ctx).vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }

    /** 进入新阶段 */
    fun phaseChange(ctx: Context) {
        vibrate(ctx, 400)
        beep(ToneGenerator.TONE_PROP_BEEP2, 350)
    }

    /** 40分钟完成 */
    fun complete(ctx: Context) {
        vibrate(ctx, 800)
        beep(ToneGenerator.TONE_CDMA_CONFIRM, 500)
    }
}
