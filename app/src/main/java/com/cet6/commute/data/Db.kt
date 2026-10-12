package com.cet6.commute.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "cet6.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE words(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "term TEXT UNIQUE NOT NULL," +
                "ipa TEXT DEFAULT ''," +
                "pos TEXT DEFAULT ''," +
                "meaning TEXT DEFAULT ''," +
                "example TEXT DEFAULT ''," +
                "isCustom INTEGER DEFAULT 0," +
                "firstSeenDay INTEGER," +
                "box INTEGER DEFAULT 0," +
                "dueDay INTEGER DEFAULT 0," +
                "rating TEXT DEFAULT '')"
        )
        db.execSQL("CREATE INDEX idx_words_due ON words(firstSeenDay, dueDay)")
        db.execSQL("CREATE TABLE checkins(day TEXT PRIMARY KEY, auto INTEGER DEFAULT 0, createdMs INTEGER)")
        db.execSQL("CREATE TABLE notes(_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT, content TEXT, createdMs INTEGER)")
        db.execSQL("CREATE TABLE articles(_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, addedMs INTEGER)")
        db.execSQL("CREATE TABLE sentences(_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT, content TEXT, createdMs INTEGER)")
        db.execSQL("CREATE TABLE audio_files(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, sizeBytes INTEGER, addedMs INTEGER, builtin INTEGER DEFAULT 0, paperId INTEGER DEFAULT 0, segOrder INTEGER DEFAULT 0, segTitle TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE papers(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, builtin INTEGER DEFAULT 0, createdMs INTEGER, content TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE recordings(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, durationMs INTEGER, createdMs INTEGER)")
        db.execSQL("CREATE TABLE listen_stats(day TEXT PRIMARY KEY, seconds INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE study_stats(day TEXT PRIMARY KEY, seconds INTEGER DEFAULT 0)")
        db.execSQL(
            "CREATE TABLE essays(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT, category TEXT, outline TEXT, content TEXT," +
                "builtin INTEGER DEFAULT 0, mastered INTEGER DEFAULT 0, addedMs INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS study_stats(day TEXT PRIMARY KEY, seconds INTEGER DEFAULT 0)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS essays(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT, category TEXT, outline TEXT, content TEXT," +
                    "builtin INTEGER DEFAULT 0, mastered INTEGER DEFAULT 0, addedMs INTEGER)"
            )
            try {
                db.execSQL("ALTER TABLE audio_files ADD COLUMN builtin INTEGER DEFAULT 0")
            } catch (_: Exception) {
            }
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS papers(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, builtin INTEGER DEFAULT 0, createdMs INTEGER)")
            for (col in listOf("paperId INTEGER DEFAULT 0", "segOrder INTEGER DEFAULT 0", "segTitle TEXT DEFAULT ''")) {
                try {
                    db.execSQL("ALTER TABLE audio_files ADD COLUMN $col")
                } catch (_: Exception) {
                }
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE papers ADD COLUMN content TEXT DEFAULT ''")
            } catch (_: Exception) {
            }
        }
    }
}
