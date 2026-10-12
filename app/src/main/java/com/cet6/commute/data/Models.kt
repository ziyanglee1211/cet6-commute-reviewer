package com.cet6.commute.data

/** 单词(含 SRS 状态) */
data class Word(
    val id: Long,
    val term: String,
    val ipa: String,
    val pos: String,
    val meaning: String,
    val example: String,
    val isCustom: Boolean,
    val firstSeenDay: Int?,   // 首次学习日期(epoch day),null=尚未学过的新词
    val box: Int,             // 0=新词 1=不会 2=模糊 3=认识 4=已掌握
    val dueDay: Int,          // 下次复现日期(epoch day)
    val rating: String        // 最近一次评价: "" / no / fuzzy / yes
)

data class NoteItem(val id: Long, val day: String, val content: String, val createdMs: Long)

data class Article(val id: Long, val title: String, val content: String, val addedMs: Long)

data class Sentence(val id: Long, val day: String, val content: String, val createdMs: Long)

data class AudioFile(
    val id: Long,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val addedMs: Long,
    val builtin: Boolean = false,
    val paperId: Long = 0,
    val segOrder: Int = 0,
    val segTitle: String = ""
)

data class Paper(
    val id: Long,
    val name: String,
    val builtin: Boolean,
    val createdMs: Long,
    val content: String = ""   // 整卷 JSON:writing/listening/reading/translation
)

data class Essay(
    val id: Long,
    val title: String,
    val category: String,
    val outline: String,
    val content: String,
    val builtin: Boolean,
    val mastered: Boolean
)
