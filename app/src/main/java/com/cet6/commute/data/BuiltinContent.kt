package com.cet6.commute.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File

/** 启动时导入内置内容:范文(按标题去重) + 模拟听力套卷 */
object BuiltinContent {

    private data class Seg(val base: String, val type: String, val topic: String)
    private data class MockPaper(val name: String, val segs: List<Seg>)

    private val MOCK_PAPERS = listOf(
        MockPaper(
            "六级模拟卷 · 第一套", listOf(
                Seg("conv1", "长对话", "实习求职"),
                Seg("pass1", "听力篇章", "拖延症的心理学"),
                Seg("lect1", "讲座", "微塑料污染")
            )
        ),
        MockPaper(
            "六级模拟卷 · 第二套", listOf(
                Seg("conv2", "长对话", "租房生活"),
                Seg("pass2", "听力篇章", "城市绿地"),
                Seg("lect2", "讲座", "注意力与深度工作")
            )
        )
    )

    /** assets/papers/ 下全部内置卷面文件(前两套为配音频模拟卷,其余为 2018-2026 年份卷) */
    private val PAPER_FILES = listOf(
        "mock1.json", "mock2.json",
        "paper_2018.json", "paper_2019.json", "paper_2020.json", "paper_2021.json",
        "paper_2022.json", "paper_2023.json", "paper_2024.json", "paper_2025.json",
        "paper_2026.json"
    )

    fun importIfNeeded(ctx: Context, db: SQLiteDatabase) {
        importEssays(ctx, db)
        importListening(ctx, db)
        importPaperContents(ctx, db)
    }

    /** 内置完整模拟卷的卷面内容(写作/听力题目/阅读/翻译+答案解析) */
    private fun importPaperContents(ctx: Context, db: SQLiteDatabase) {
        for (f in PAPER_FILES) {
            try {
                val json = ctx.assets.open("papers/$f").bufferedReader().use { it.readText() }
                val o = org.json.JSONObject(json)
                val name = o.getString("name")
                var paperId = db.rawQuery("SELECT _id FROM papers WHERE name=? AND builtin=1 LIMIT 1", arrayOf(name))
                    .use { if (it.moveToFirst()) it.getLong(0) else 0L }
                // 找不到记录则新建(年份卷无内置音频,只建卷面)
                if (paperId == 0L) paperId = Repo.createPaper(db, name, builtin = true)
                val blank = db.rawQuery("SELECT content FROM papers WHERE _id=? AND builtin=1", arrayOf(paperId.toString()))
                    .use { it.moveToFirst() && (it.getString(0).isNullOrBlank()) }
                if (blank) Repo.updatePaperContent(db, paperId, json)
            } catch (_: Exception) {
            }
        }
    }

    private fun importEssays(ctx: Context, db: SQLiteDatabase) {
        try {
            val json = ctx.assets.open("essays.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val title = o.getString("title")
                // 按标题去重:升级安装时只导入新增的内置范文
                val exists = db.rawQuery("SELECT 1 FROM essays WHERE title=? AND builtin=1 LIMIT 1", arrayOf(title))
                    .use { it.moveToFirst() }
                if (exists) continue
                Repo.addEssay(
                    db,
                    title,
                    o.optString("category", "范文"),
                    o.optString("outline"),
                    o.getString("content"),
                    builtin = true
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun importListening(ctx: Context, db: SQLiteDatabase) {
        val dir = File(ctx.filesDir, "audio").apply { mkdirs() }
        for (paper in MOCK_PAPERS) {
            try {
                // 找内置同名套卷,没有则建
                var paperId = db.rawQuery("SELECT _id FROM papers WHERE name=? AND builtin=1 LIMIT 1", arrayOf(paper.name))
                    .use { if (it.moveToFirst()) it.getLong(0) else 0L }
                if (paperId == 0L) paperId = Repo.createPaper(db, paper.name, builtin = true)

                for ((order, seg) in paper.segs.withIndex()) {
                    val wav = File(dir, "${seg.base}.wav")
                    val txt = File(dir, "${seg.base}.txt")
                    if (!wav.exists()) {
                        ctx.assets.open("listen/${seg.base}.wav").use { input ->
                            wav.outputStream().use { input.copyTo(it) }
                        }
                    }
                    if (!txt.exists()) {
                        ctx.assets.open("listen/${seg.base}.txt").use { input ->
                            txt.outputStream().use { input.copyTo(it) }
                        }
                    }
                    val title = "内置精听 · ${seg.type}:${seg.topic}"
                    val existing = db.rawQuery("SELECT _id FROM audio_files WHERE path=? LIMIT 1", arrayOf(wav.absolutePath))
                        .use { if (it.moveToFirst()) it.getLong(0) else 0L }
                    if (existing != 0L) {
                        // v1.2 平铺的内置音频 → 归入套卷
                        Repo.assignAudioToPaper(db, existing, paperId, order + 1, seg.type)
                    } else {
                        Repo.addAudio(db, title, wav.absolutePath, wav.length(), builtin = true, paperId = paperId, segOrder = order + 1, segTitle = seg.type)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
