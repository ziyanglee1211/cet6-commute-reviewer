package com.cet6.commute

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import com.cet6.commute.data.DbHelper
import com.cet6.commute.data.WordImporter
import com.cet6.commute.logic.SessionTimer
import com.cet6.commute.tts.Tts

class App : Application() {

    lateinit var db: SQLiteDatabase
        private set
    lateinit var prefs: SharedPreferences
        private set

    /** 0=跟随系统 1=浅色 2=深色 */
    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0)
        set(v) = prefs.edit().putInt("theme_mode", v).apply()

    /** 字号档位 0小 1标准 2大 3特大 */
    var fontScaleLevel: Int
        get() = prefs.getInt("font_scale_level", 1)
        set(v) = prefs.edit().putInt("font_scale_level", v).apply()

    /** 每轮新词数量上限 */
    var newWordLimit: Int
        get() = prefs.getInt("new_word_limit", 20)
        set(v) = prefs.edit().putInt("new_word_limit", v).apply()

    /** 闪卡翻牌震动反馈 */
    var vibrateOnFlip: Boolean
        get() = prefs.getBoolean("vibrate_flip", true)
        set(v) = prefs.edit().putBoolean("vibrate_flip", v).apply()

    override fun onCreate() {
        super.onCreate()
        db = DbHelper(this).writableDatabase
        prefs = getSharedPreferences("cet6", Context.MODE_PRIVATE)
        Tts.init(this)
        Thread {
            WordImporter.importIfNeeded(this, db)
            com.cet6.commute.data.BuiltinContent.importIfNeeded(this, db)
        }.start()
        SessionTimer.init(this)
    }
}
