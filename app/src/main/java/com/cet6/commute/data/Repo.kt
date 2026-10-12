package com.cet6.commute.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

/** 所有本地数据读写(纯 SQLite,无网络) */
object Repo {

    // ---------- 词库 / SRS ----------

    private fun cursorWords(c: Cursor): List<Word> {
        val out = ArrayList<Word>()
        c.use {
            while (it.moveToNext()) {
                val fs = if (it.isNull(it.getColumnIndexOrThrow("firstSeenDay"))) null
                else it.getInt(it.getColumnIndexOrThrow("firstSeenDay"))
                out.add(
                    Word(
                        id = it.getLong(it.getColumnIndexOrThrow("_id")),
                        term = it.getString(it.getColumnIndexOrThrow("term")),
                        ipa = it.getString(it.getColumnIndexOrThrow("ipa")),
                        pos = it.getString(it.getColumnIndexOrThrow("pos")),
                        meaning = it.getString(it.getColumnIndexOrThrow("meaning")),
                        example = it.getString(it.getColumnIndexOrThrow("example")),
                        isCustom = it.getInt(it.getColumnIndexOrThrow("isCustom")) == 1,
                        firstSeenDay = fs,
                        box = it.getInt(it.getColumnIndexOrThrow("box")),
                        dueDay = it.getInt(it.getColumnIndexOrThrow("dueDay")),
                        rating = it.getString(it.getColumnIndexOrThrow("rating"))
                    )
                )
            }
        }
        return out
    }

    private fun wordAt(db: SQLiteDatabase, id: Long): Word? =
        db.rawQuery("SELECT * FROM words WHERE _id=?", arrayOf(id.toString())).use { cursorWords(it) }.firstOrNull()

    /** 复习队列:已学过且到期 */
    fun reviewQueue(db: SQLiteDatabase, today: Int): List<Word> =
        db.rawQuery(
            "SELECT * FROM words WHERE firstSeenDay IS NOT NULL AND dueDay<=? ORDER BY dueDay ASC, _id ASC",
            arrayOf(today.toString())
        ).use { cursorWords(it) }

    /** 新词随机抽 limit 个 */
    fun newWords(db: SQLiteDatabase, limit: Int): List<Word> =
        if (limit <= 0) emptyList()
        else db.rawQuery(
            "SELECT * FROM words WHERE firstSeenDay IS NULL ORDER BY RANDOM() LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursorWords(it) }

    fun todayNewCount(db: SQLiteDatabase, today: Int): Int =
        db.rawQuery("SELECT COUNT(*) FROM words WHERE firstSeenDay=?", arrayOf(today.toString())).use {
            it.moveToFirst(); it.getInt(0)
        }

    /** 间隔重复:不会→1天 模糊→3天 认识→7天(已掌握词再认识→30天) */
    fun rateWord(db: SQLiteDatabase, id: Long, rating: String, today: Int) {
        val w = wordAt(db, id) ?: return
        val (newBox, delta) = when (rating) {
            "no" -> 1 to 1
            "fuzzy" -> 2 to 3
            else -> if (w.box >= 3) 4 to 30 else 3 to 7
        }
        db.execSQL(
            "UPDATE words SET box=?, dueDay=?, rating=?, firstSeenDay=COALESCE(firstSeenDay,?) WHERE _id=?",
            arrayOf(newBox, today + delta, rating, today, id)
        )
    }

    /** 生词本:最近评价为"不会/模糊"的词 */
    fun wordbook(db: SQLiteDatabase): List<Word> =
        db.rawQuery("SELECT * FROM words WHERE rating IN ('no','fuzzy') ORDER BY rowid DESC", null).use { cursorWords(it) }

    /** 点词查询(大小写不敏感) */
    fun lookup(db: SQLiteDatabase, term: String): Word? =
        db.rawQuery("SELECT * FROM words WHERE term=? COLLATE NOCASE LIMIT 1", arrayOf(term.trim())).use { cursorWords(it) }.firstOrNull()

    fun countLearned(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM words WHERE firstSeenDay IS NOT NULL", null).use { it.moveToFirst(); it.getInt(0) }

    fun countAll(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM words", null).use { it.moveToFirst(); it.getInt(0) }

    /** 用户粘贴导入自定义词表,返回 (导入数, 跳过重复数) */
    fun addCustomWords(db: SQLiteDatabase, text: String): Pair<Int, Int> {
        var ok = 0; var skip = 0
        db.beginTransaction()
        try {
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                val parts = line.split(",", "，", "\t")
                if (parts.size < 2) { skip++; continue }
                val term = parts[0].trim()
                val meaning = parts.subList(1, parts.size).joinToString(",").trim()
                if (term.isEmpty() || meaning.isEmpty()) { skip++; continue }
                val cv = ContentValues().apply {
                    put("term", term); put("meaning", meaning); put("isCustom", 1)
                }
                if (db.insertWithOnConflict("words", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1L) ok++ else skip++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ok to skip
    }

    // ---------- 打卡 ----------

    fun checkin(db: SQLiteDatabase, auto: Boolean) {
        val day = LocalDate.now().toString()
        db.execSQL("INSERT OR IGNORE INTO checkins(day, auto, createdMs) VALUES(?,?,?)",
            arrayOf(day, if (auto) 1 else 0, System.currentTimeMillis()))
    }

    fun isCheckedIn(db: SQLiteDatabase, day: String): Boolean =
        db.rawQuery("SELECT 1 FROM checkins WHERE day=?", arrayOf(day)).use { it.moveToFirst() }

    fun allCheckinDays(db: SQLiteDatabase): Set<String> =
        db.rawQuery("SELECT day FROM checkins", null).use { c ->
            val s = HashSet<String>(); c.use { while (it.moveToNext()) s.add(it.getString(0)) }; s
        }

    fun monthCheckinDays(db: SQLiteDatabase, ymPrefix: String): Set<String> =
        db.rawQuery("SELECT day FROM checkins WHERE day LIKE ?", arrayOf("$ymPrefix%")).use { c ->
            val s = HashSet<String>(); c.use { while (it.moveToNext()) s.add(it.getString(0)) }; s
        }

    /** 连续打卡天数(含今天或昨天起算) */
    fun streak(checkinDays: Set<String>): Int {
        if (checkinDays.isEmpty()) return 0
        var d = LocalDate.now()
        if (d.toString() !in checkinDays) {
            d = d.minusDays(1)
            if (d.toString() !in checkinDays) return 0
        }
        var n = 0
        while (d.toString() in checkinDays) { n++; d = d.minusDays(1) }
        return n
    }

    // ---------- 听力统计 ----------

    fun addListenSeconds(db: SQLiteDatabase, sec: Int) {
        val day = LocalDate.now().toString()
        db.execSQL("INSERT OR IGNORE INTO listen_stats(day, seconds) VALUES(?,0)", arrayOf(day))
        db.execSQL("UPDATE listen_stats SET seconds=seconds+? WHERE day=?", arrayOf(sec, day))
    }

    fun totalListenSeconds(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COALESCE(SUM(seconds),0) FROM listen_stats", null).use { it.moveToFirst(); it.getInt(0) }

    // ---------- 笔记 ----------

    fun addNote(db: SQLiteDatabase, content: String) {
        db.execSQL("INSERT INTO notes(day, content, createdMs) VALUES(?,?,?)",
            arrayOf(LocalDate.now().toString(), content.trim(), System.currentTimeMillis()))
    }

    fun allNotes(db: SQLiteDatabase): List<NoteItem> =
        db.rawQuery("SELECT * FROM notes ORDER BY day DESC, createdMs DESC", null).use { c ->
            val out = ArrayList<NoteItem>()
            c.use {
                while (it.moveToNext()) out.add(
                    NoteItem(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("day")),
                        it.getString(it.getColumnIndexOrThrow("content")),
                        it.getLong(it.getColumnIndexOrThrow("createdMs"))
                    )
                )
            }
            out
        }

    fun deleteNote(db: SQLiteDatabase, id: Long) { db.execSQL("DELETE FROM notes WHERE _id=?", arrayOf(id)) }

    // ---------- 文章 ----------

    fun addArticle(db: SQLiteDatabase, title: String, content: String) {
        db.execSQL("INSERT INTO articles(title, content, addedMs) VALUES(?,?,?)",
            arrayOf(title.ifBlank { "未命名文章" }, content, System.currentTimeMillis()))
    }

    fun allArticles(db: SQLiteDatabase): List<Article> =
        db.rawQuery("SELECT * FROM articles ORDER BY addedMs DESC", null).use { c ->
            val out = ArrayList<Article>()
            c.use {
                while (it.moveToNext()) out.add(
                    Article(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("title")),
                        it.getString(it.getColumnIndexOrThrow("content")),
                        it.getLong(it.getColumnIndexOrThrow("addedMs"))
                    )
                )
            }
            out
        }

    fun deleteArticle(db: SQLiteDatabase, id: Long) { db.execSQL("DELETE FROM articles WHERE _id=?", arrayOf(id)) }

    // ---------- 每日一句 ----------

    fun addSentence(db: SQLiteDatabase, content: String) {
        db.execSQL("INSERT INTO sentences(day, content, createdMs) VALUES(?,?,?)",
            arrayOf(LocalDate.now().toString(), content.trim(), System.currentTimeMillis()))
    }

    fun allSentences(db: SQLiteDatabase): List<Sentence> =
        db.rawQuery("SELECT * FROM sentences ORDER BY createdMs DESC", null).use { c ->
            val out = ArrayList<Sentence>()
            c.use {
                while (it.moveToNext()) out.add(
                    Sentence(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("day")),
                        it.getString(it.getColumnIndexOrThrow("content")),
                        it.getLong(it.getColumnIndexOrThrow("createdMs"))
                    )
                )
            }
            out
        }

    fun deleteSentence(db: SQLiteDatabase, id: Long) { db.execSQL("DELETE FROM sentences WHERE _id=?", arrayOf(id)) }

    // ---------- 音频文件 ----------

    fun addAudio(
        db: SQLiteDatabase, name: String, path: String, size: Long,
        builtin: Boolean = false, paperId: Long = 0, segOrder: Int = 0, segTitle: String = ""
    ) {
        db.execSQL("INSERT INTO audio_files(name, path, sizeBytes, addedMs, builtin, paperId, segOrder, segTitle) VALUES(?,?,?,?,?,?,?,?)",
            arrayOf(name, path, size, System.currentTimeMillis(), if (builtin) 1 else 0, paperId, segOrder, segTitle))
    }

    private fun cursorAudio(c: Cursor): List<AudioFile> {
        val out = ArrayList<AudioFile>()
        c.use {
            while (it.moveToNext()) {
                fun ci(n: String): Int = it.getColumnIndex(n)
                val paperId = if (ci("paperId") >= 0 && !it.isNull(ci("paperId"))) it.getLong(ci("paperId")) else 0L
                val segOrder = if (ci("segOrder") >= 0 && !it.isNull(ci("segOrder"))) it.getInt(ci("segOrder")) else 0
                val segTitle = if (ci("segTitle") >= 0 && !it.isNull(ci("segTitle"))) it.getString(ci("segTitle")) else ""
                out.add(
                    AudioFile(
                        it.getLong(ci("_id")),
                        it.getString(ci("name")),
                        it.getString(ci("path")),
                        it.getLong(ci("sizeBytes")),
                        it.getLong(ci("addedMs")),
                        ci("builtin") >= 0 && it.getInt(ci("builtin")) == 1,
                        paperId,
                        segOrder,
                        segTitle
                    )
                )
            }
        }
        return out
    }

    fun allAudio(db: SQLiteDatabase): List<AudioFile> =
        db.rawQuery("SELECT * FROM audio_files ORDER BY builtin DESC, addedMs DESC", null).use { cursorAudio(it) }

    fun audioById(db: SQLiteDatabase, id: Long): AudioFile? =
        db.rawQuery("SELECT * FROM audio_files WHERE _id=?", arrayOf(id.toString())).use { cursorAudio(it) }.firstOrNull()

    fun deleteAudio(db: SQLiteDatabase, id: Long) { db.execSQL("DELETE FROM audio_files WHERE _id=?", arrayOf(id)) }

    fun audioCount(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM audio_files", null).use { it.moveToFirst(); it.getInt(0) }

    // ---------- 真题套卷 ----------

    private fun cursorPapers(c: Cursor): List<Paper> {
        val out = ArrayList<Paper>()
        c.use {
            while (it.moveToNext()) {
                val coi = it.getColumnIndex("content")
                out.add(
                    Paper(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("name")),
                        it.getInt(it.getColumnIndexOrThrow("builtin")) == 1,
                        it.getLong(it.getColumnIndexOrThrow("createdMs")),
                        if (coi >= 0 && !it.isNull(coi)) it.getString(coi) else ""
                    )
                )
            }
        }
        return out
    }

    fun allPapers(db: SQLiteDatabase): List<Paper> =
        db.rawQuery("SELECT * FROM papers ORDER BY builtin DESC, createdMs DESC", null).use { cursorPapers(it) }

    fun paperById(db: SQLiteDatabase, id: Long): Paper? =
        db.rawQuery("SELECT * FROM papers WHERE _id=?", arrayOf(id.toString())).use { cursorPapers(it) }.firstOrNull()

    fun createPaper(db: SQLiteDatabase, name: String, builtin: Boolean = false): Long {
        db.execSQL("INSERT INTO papers(name, builtin, createdMs) VALUES(?,?,?)",
            arrayOf(name, if (builtin) 1 else 0, System.currentTimeMillis()))
        return db.rawQuery("SELECT last_insert_rowid()", null).use { it.moveToFirst(); it.getLong(0) }
    }

    fun deletePaper(db: SQLiteDatabase, id: Long) {
        db.execSQL("UPDATE audio_files SET paperId=0, segOrder=0, segTitle='' WHERE paperId=?", arrayOf(id))
        db.execSQL("DELETE FROM papers WHERE _id=? AND builtin=0", arrayOf(id))
    }

    fun segments(db: SQLiteDatabase, paperId: Long): List<AudioFile> =
        db.rawQuery("SELECT * FROM audio_files WHERE paperId=? ORDER BY segOrder ASC, _id ASC", arrayOf(paperId.toString()))
            .use { cursorAudio(it) }

    fun nextSegOrder(db: SQLiteDatabase, paperId: Long): Int =
        db.rawQuery("SELECT COALESCE(MAX(segOrder),0)+1 FROM audio_files WHERE paperId=?", arrayOf(paperId.toString()))
            .use { it.moveToFirst(); it.getInt(0) }

    fun assignAudioToPaper(db: SQLiteDatabase, audioId: Long, paperId: Long, segOrder: Int, segTitle: String) {
        db.execSQL("UPDATE audio_files SET paperId=?, segOrder=?, segTitle=? WHERE _id=?",
            arrayOf(paperId, segOrder, segTitle, audioId))
    }

    /** 写入/替换整卷内容 JSON(写作/听力/阅读/翻译) */
    fun updatePaperContent(db: SQLiteDatabase, paperId: Long, content: String) {
        db.execSQL("UPDATE papers SET content=? WHERE _id=?", arrayOf(content, paperId))
    }

    // ---------- 范文 / 作文背诵 ----------

    private fun cursorEssays(c: Cursor): List<Essay> {
        val out = ArrayList<Essay>()
        c.use {
            while (it.moveToNext()) {
                out.add(
                    Essay(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("title")),
                        it.getString(it.getColumnIndexOrThrow("category")),
                        it.getString(it.getColumnIndexOrThrow("outline")),
                        it.getString(it.getColumnIndexOrThrow("content")),
                        it.getInt(it.getColumnIndexOrThrow("builtin")) == 1,
                        it.getInt(it.getColumnIndexOrThrow("mastered")) == 1
                    )
                )
            }
        }
        return out
    }

    fun allEssays(db: SQLiteDatabase): List<Essay> =
        db.rawQuery("SELECT * FROM essays ORDER BY builtin DESC, _id ASC", null).use { cursorEssays(it) }

    fun essayById(db: SQLiteDatabase, id: Long): Essay? =
        db.rawQuery("SELECT * FROM essays WHERE _id=?", arrayOf(id.toString())).use { cursorEssays(it) }.firstOrNull()

    fun addEssay(db: SQLiteDatabase, title: String, category: String, outline: String, content: String, builtin: Boolean = false) {
        db.execSQL("INSERT INTO essays(title, category, outline, content, builtin, mastered, addedMs) VALUES(?,?,?,?,?,0,?)",
            arrayOf(title, category, outline, content, if (builtin) 1 else 0, System.currentTimeMillis()))
    }

    fun setEssayMastered(db: SQLiteDatabase, id: Long, mastered: Boolean) {
        db.execSQL("UPDATE essays SET mastered=? WHERE _id=?", arrayOf(if (mastered) 1 else 0, id))
    }

    fun deleteEssay(db: SQLiteDatabase, id: Long) {
        db.execSQL("DELETE FROM essays WHERE _id=? AND builtin=0", arrayOf(id))
    }

    // ---------- 学习时长(词汇/阅读等主动学习) ----------

    fun addStudySeconds(db: SQLiteDatabase, sec: Int) {
        val day = LocalDate.now().toString()
        db.execSQL("INSERT OR IGNORE INTO study_stats(day, seconds) VALUES(?,0)", arrayOf(day))
        db.execSQL("UPDATE study_stats SET seconds=seconds+? WHERE day=?", arrayOf(sec, day))
    }

    /** 某日 [fromDay, toDay] 区间内(含端点,yyyy-MM-dd)累计学习秒数 */
    fun studySecondsBetween(db: SQLiteDatabase, from: String, to: String): Int =
        db.rawQuery("SELECT COALESCE(SUM(seconds),0) FROM study_stats WHERE day BETWEEN ? AND ?", arrayOf(from, to))
            .use { it.moveToFirst(); it.getInt(0) }

    fun listenSecondsBetween(db: SQLiteDatabase, from: String, to: String): Int =
        db.rawQuery("SELECT COALESCE(SUM(seconds),0) FROM listen_stats WHERE day BETWEEN ? AND ?", arrayOf(from, to))
            .use { it.moveToFirst(); it.getInt(0) }

    fun wordsSeenBetween(db: SQLiteDatabase, fromEpochDay: Int, toEpochDay: Int): Int =
        db.rawQuery("SELECT COUNT(*) FROM words WHERE firstSeenDay BETWEEN ? AND ?",
            arrayOf(fromEpochDay.toString(), toEpochDay.toString()))
            .use { it.moveToFirst(); it.getInt(0) }

    fun checkinCountBetween(db: SQLiteDatabase, from: String, to: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM checkins WHERE day BETWEEN ? AND ?", arrayOf(from, to))
            .use { it.moveToFirst(); it.getInt(0) }

    /** 某一天的学习秒数(学习+听力) */
    fun studySecondsOn(db: SQLiteDatabase, day: String): Int =
        db.rawQuery("SELECT COALESCE((SELECT seconds FROM study_stats WHERE day=?),0) + COALESCE((SELECT seconds FROM listen_stats WHERE day=?),0)",
            arrayOf(day, day)).use { it.moveToFirst(); it.getInt(0) }

    fun wordsSeenOn(db: SQLiteDatabase, epochDay: Int): Int =
        db.rawQuery("SELECT COUNT(*) FROM words WHERE firstSeenDay=?", arrayOf(epochDay.toString()))
            .use { it.moveToFirst(); it.getInt(0) }

    // ---------- 汇总统计 ----------

    /**
     * 清空学习记录(设置页「清空学习记录」用):
     * 打卡日历、学习/听力时长统计、词汇进度(2000词恢复未学习状态)。
     * 保留生词本(rating)、自定义词表与范文。
     */
    fun clearStudyProgress(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM checkins")
            db.execSQL("DELETE FROM study_stats")
            db.execSQL("DELETE FROM listen_stats")
            db.execSQL("UPDATE words SET firstSeenDay=NULL, box=0, dueDay=0")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** 累计学习天数 = 打卡日 ∪ 首学单词日 ∪ 听力日 */
    fun totalStudyDays(db: SQLiteDatabase): Int {
        val days = HashSet<String>()
        days.addAll(allCheckinDays(db))
        db.rawQuery("SELECT DISTINCT firstSeenDay FROM words WHERE firstSeenDay IS NOT NULL", null).use {
            while (it.moveToNext()) days.add(LocalDate.ofEpochDay(it.getLong(0)).toString())
        }
        db.rawQuery("SELECT day FROM listen_stats", null).use { while (it.moveToNext()) days.add(it.getString(0)) }
        return days.size
    }

    fun millisToDay(ms: Long): String =
        java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
}
