package com.cet6.commute.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** 启动时把 assets/words/ 下所有词库文件导入 SQLite(按文件记录,升级自动导入新增词包) */
object WordImporter {

    fun importIfNeeded(ctx: Context, db: SQLiteDatabase) {
        val prefs = ctx.getSharedPreferences("cet6", Context.MODE_PRIVATE)
        val files = ctx.assets.list("words")?.filter { it.endsWith(".json") }?.sorted() ?: return
        for (f in files) {
            val flag = "imported_words/$f"
            if (prefs.getBoolean(flag, false)) continue
            try {
                val json = ctx.assets.open("words/$f").bufferedReader().use { it.readText() }
                val arr = org.json.JSONArray(json)
                db.beginTransaction()
                try {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            put("term", o.getString("term"))
                            put("ipa", o.optString("ipa"))
                            put("pos", o.optString("pos"))
                            put("meaning", o.getString("meaning"))
                            put("example", o.optString("example"))
                            put("isCustom", 0)
                        }
                        db.insertWithOnConflict("words", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                prefs.edit().putBoolean(flag, true).apply()
            } catch (_: Exception) {
                // 单个词库文件损坏不影响其余导入
            }
        }
        prefs.edit().putBoolean("words_imported", true).apply()
    }
}
