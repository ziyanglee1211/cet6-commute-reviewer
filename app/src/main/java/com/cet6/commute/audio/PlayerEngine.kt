package com.cet6.commute.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 本地音频播放引擎:倍速 / A-B循环 / ±10s / 进度。
 * listenSec 回调用于累计每日听力时长。
 */
class PlayerEngine(private val scope: CoroutineScope) {

    data class Ui(
        val loading: Boolean = false,
        val playing: Boolean = false,
        val durationMs: Int = 0,
        val positionMs: Int = 0,
        val speed: Float = 1.0f,
        val aMs: Int = -1,
        val bMs: Int = -1,
        val loaded: Boolean = false
    ) {
        val abActive: Boolean get() = aMs >= 0 && bMs > aMs
    }

    private val _st = MutableStateFlow(Ui())
    val st: StateFlow<Ui> = _st.asStateFlow()

    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    var onListenSeconds: ((Int) -> Unit)? = null   // 每累计1秒回调一次
    var onEnded: (() -> Unit)? = null              // 播放自然结束(用于套卷连播)

    fun load(path: String) {
        release()
        _st.value = Ui(loading = true)
        scope.launch(Dispatchers.IO) {
            try {
                val p = MediaPlayer()
                p.setDataSource(path)
                p.prepare()
                p.setOnCompletionListener {
                    _st.value = _st.value.copy(playing = false, positionMs = _st.value.durationMs)
                    stopTicker()
                    onEnded?.invoke()
                }
                player = p
                _st.value = _st.value.copy(loading = false, loaded = true, durationMs = p.duration, positionMs = 0)
            } catch (e: Exception) {
                _st.value = Ui() // 加载失败,重置
            }
        }
    }

    fun toggle() {
        val p = player ?: return
        try {
            if (p.isPlaying) {
                p.pause()
                _st.value = _st.value.copy(playing = false)
                stopTicker()
            } else {
                p.start()
                _st.value = _st.value.copy(playing = true)
                startTicker()
            }
        } catch (_: Exception) {
        }
    }

    fun seekTo(ms: Int) {
        val p = player ?: return
        try {
            p.seekTo(ms.coerceIn(0, _st.value.durationMs))
            _st.value = _st.value.copy(positionMs = ms.coerceIn(0, _st.value.durationMs))
        } catch (_: Exception) {
        }
    }

    fun skip(deltaSec: Int) = seekTo(_st.value.positionMs + deltaSec * 1000)

    /** 切换倍速(保持播放状态与进度) */
    fun setSpeed(speed: Float) {
        val p = player ?: return
        try {
            val wasPlaying = p.isPlaying
            val params = p.playbackParams
            params.speed = speed
            p.playbackParams = params   // 注意:此调用在部分系统上会隐式开始播放
            if (!wasPlaying) p.pause()
            _st.value = _st.value.copy(speed = speed, playing = wasPlaying)
            if (wasPlaying) startTicker() else stopTicker()
        } catch (_: Exception) {
        }
    }

    fun markA() { _st.value = _st.value.copy(aMs = _st.value.positionMs) }
    fun markB() {
        val a = _st.value.aMs
        val b = _st.value.positionMs
        if (a >= 0 && b <= a) return // B必须大于A
        _st.value = _st.value.copy(bMs = b)
    }
    fun clearAB() { _st.value = _st.value.copy(aMs = -1, bMs = -1) }

    private fun startTicker() {
        stopTicker()
        var acc = 0.0
        ticker = scope.launch(Dispatchers.Default) {
            while (true) {
                delay(200)
                val p = player ?: break
                try {
                    if (!p.isPlaying) continue
                    val pos = p.currentPosition
                    _st.value = _st.value.copy(positionMs = pos)
                    // 累计听力时长
                    acc += 0.2
                    if (acc >= 1.0) {
                        onListenSeconds?.invoke(acc.toInt())
                        acc -= acc.toInt()
                    }
                    // A-B 循环
                    val u = _st.value
                    if (u.abActive && pos >= u.bMs) {
                        p.seekTo(u.aMs)
                        _st.value = _st.value.copy(positionMs = u.aMs)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopTicker() { ticker?.cancel(); ticker = null }

    fun release() {
        stopTicker()
        try { player?.release() } catch (_: Exception) {}
        player = null
        _st.value = Ui()
    }
}
