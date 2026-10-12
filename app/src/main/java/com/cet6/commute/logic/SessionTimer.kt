package com.cet6.commute.logic

import android.content.Context
import com.cet6.commute.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 40分钟三阶段总控计时器。
 * 阶段1 0-10min 词汇闪卡 | 阶段2 10-30min 听力精听 | 阶段3 30-40min 写作
 * 进度持久化到 prefs:应用被杀后重开可从上次进度继续(通勤中断场景)。
 */
object SessionTimer {
    const val TOTAL_SEC = 40 * 60

    val PHASES = listOf(
        Phase("词汇闪卡", 0, 600),
        Phase("听力精听", 600, 1800),
        Phase("写作", 1800, 2400)
    )

    data class Phase(val name: String, val startSec: Int, val endSec: Int) {
        val durationSec: Int get() = endSec - startSec
    }

    data class State(val running: Boolean = false, val elapsedSec: Int = 0, val done: Boolean = false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _st = MutableStateFlow(State())
    val st: StateFlow<State> = _st.asStateFlow()

    private var app: Context? = null
    private var job: Job? = null

    fun init(application: Context) {
        app = application.applicationContext
        val prefs = app!!.getSharedPreferences("cet6", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString("st_day", "") == today) {
            val e = prefs.getInt("st_elapsed", 0)
            val r = prefs.getBoolean("st_running", false)
            _st.value = State(running = false, elapsedSec = e.coerceAtMost(TOTAL_SEC), done = e >= TOTAL_SEC)
            if (r && !_st.value.done) start() // 上次在计时中,自动继续(离线期间视为暂停)
        } else {
            prefs.edit().remove("st_elapsed").remove("st_running").remove("st_day").apply()
        }
    }

    fun toggle() = if (_st.value.running) pause() else start()

    fun start() {
        if (_st.value.done || _st.value.running) return
        _st.value = _st.value.copy(running = true)
        save()
        loop()
    }

    fun pause() {
        _st.value = _st.value.copy(running = false)
        save()
        job?.cancel()
    }

    fun reset() {
        job?.cancel()
        _st.value = State()
        app?.getSharedPreferences("cet6", Context.MODE_PRIVATE)?.edit()
            ?.remove("st_elapsed")?.remove("st_running")?.remove("st_day")?.apply()
    }

    fun phaseIndex(sec: Int): Int = when {
        sec < 600 -> 0
        sec < 1800 -> 1
        else -> 2
    }

    private fun loop() {
        job?.cancel()
        job = scope.launch {
            while (_st.value.running && _st.value.elapsedSec < TOTAL_SEC) {
                delay(1000)
                val s = _st.value
                val next = s.elapsedSec + 1
                _st.value = s.copy(elapsedSec = next)
                val ctx = app
                if (ctx != null) {
                    if (next == 600 || next == 1800) Alert.phaseChange(ctx)
                    if (next >= TOTAL_SEC) {
                        _st.value = _st.value.copy(running = false, done = true)
                        Alert.complete(ctx)
                        Repo.checkin((ctx as com.cet6.commute.App).db, auto = true)
                        clearSave()
                        return@launch
                    }
                }
                save()
            }
        }
    }

    private fun save() {
        val ctx = app ?: return
        ctx.getSharedPreferences("cet6", Context.MODE_PRIVATE).edit()
            .putInt("st_elapsed", _st.value.elapsedSec)
            .putBoolean("st_running", _st.value.running)
            .putString("st_day", LocalDate.now().toString())
            .apply()
    }

    private fun clearSave() {
        val ctx = app ?: return
        ctx.getSharedPreferences("cet6", Context.MODE_PRIVATE).edit()
            .remove("st_elapsed").remove("st_running").apply()
    }
}
