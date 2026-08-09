package com.localaipainter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localaipainter.data.dao.*
import com.localaipainter.data.entity.*

/**
 * Room 数据库 —— 统一入口
 *
 * v3.7 合并所有实体与 DAO，单一定义。
 *
 * 实体：
 *   - ModelEntity      (models)        主模型
 *   - HistoryEntity    (history)       旧版历史（保留兼容）
 *   - GenerationConfig (gen_configs)   生成配置
 *   - LoraEntity       (loras)         LoRA 模型
 *   - TaskEntity       (tasks)         任务队列
 *   - GenerationHistory(generation_history) 新版历史
 *
 * 版本历史：
 *   v1 → v2 : 新增 ModelEntity + 4 列 (isFavorite/lastUsedAt/description/thumbnailPath)
 *   v2 → v3 : 新增 GenerationHistory + GenerationConfig + LoraEntity + TaskEntity
 */
@Database(
    entities = [
        ModelEntity::class,
        HistoryEntity::class,
        GenerationConfig::class,
        LoraEntity::class,
        TaskEntity::class,
        GenerationHistory::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // ─── DAOs ─────────────────────────────────────────
    abstract fun modelDao(): ModelDao
    abstract fun historyDao(): HistoryDao
    abstract fun generationConfigDao(): GenerationConfigDao
    abstract fun loraDao(): LoraDao
    abstract fun taskDao(): TaskDao
    abstract fun generationHistoryDao(): GenerationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 → v2 迁移：ModelEntity 新增 4 列
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE models ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE models ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE models ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE models ADD COLUMN thumbnailPath TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v2 → v3 迁移：新增 generation_history / gen_configs / loras / tasks 表
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS generation_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        prompt TEXT NOT NULL DEFAULT '',
                        negativePrompt TEXT NOT NULL DEFAULT '',
                        modelPath TEXT NOT NULL DEFAULT '',
                        vaePath TEXT NOT NULL DEFAULT '',
                        scheduler TEXT NOT NULL DEFAULT 'euler_a',
                        steps INTEGER NOT NULL DEFAULT 30,
                        cfgScale REAL NOT NULL DEFAULT 7.5,
                        seed INTEGER NOT NULL DEFAULT -1,
                        width INTEGER NOT NULL DEFAULT 512,
                        height INTEGER NOT NULL DEFAULT 512,
                        batchSize INTEGER NOT NULL DEFAULT 1,
                        clipSkip INTEGER NOT NULL DEFAULT 1,
                        denoisingStrength REAL NOT NULL DEFAULT 1.0,
                        loraInfo TEXT NOT NULL DEFAULT '',
                        controlNetType TEXT NOT NULL DEFAULT '',
                        controlNetStrength REAL NOT NULL DEFAULT 0.0,
                        upscaleFactor INTEGER NOT NULL DEFAULT 1,
                        faceRestore TEXT NOT NULL DEFAULT '',
                        outputPath TEXT NOT NULL DEFAULT '',
                        outputCount INTEGER NOT NULL DEFAULT 1,
                        generationTimeMs INTEGER NOT NULL DEFAULT 0,
                        backend TEXT NOT NULL DEFAULT 'CPU',
                        threads INTEGER NOT NULL DEFAULT 4,
                        powerMode TEXT NOT NULL DEFAULT 'BALANCED',
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        userRating INTEGER NOT NULL DEFAULT 0,
                        userNote TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS gen_configs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        prompt TEXT NOT NULL DEFAULT '',
                        negativePrompt TEXT NOT NULL DEFAULT '',
                        modelId INTEGER NOT NULL DEFAULT 0,
                        steps INTEGER NOT NULL DEFAULT 30,
                        cfgScale REAL NOT NULL DEFAULT 7.5,
                        width INTEGER NOT NULL DEFAULT 512,
                        height INTEGER NOT NULL DEFAULT 512,
                        seed INTEGER NOT NULL DEFAULT -1,
                        scheduler TEXT NOT NULL DEFAULT 'euler_a',
                        clipSkip INTEGER NOT NULL DEFAULT 1,
                        batchSize INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS loras (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'LoRA',
                        rank INTEGER NOT NULL DEFAULT 16,
                        triggerWords TEXT NOT NULL DEFAULT '',
                        fileSize INTEGER NOT NULL DEFAULT 0,
                        verified INTEGER NOT NULL DEFAULT 0,
                        importedAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        payload TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        priority INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        startedAt INTEGER NOT NULL DEFAULT 0,
                        finishedAt INTEGER NOT NULL DEFAULT 0,
                        errorMsg TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_ai_painter.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// =====================================================================
//  TypeConverters
// =====================================================================

class Converters {
    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>): String =
        value.joinToString("||")

    @androidx.room.TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("||")

    @androidx.room.TypeConverter
    fun fromLong(value: Long): Long = value

    @androidx.room.TypeConverter
    fun toLong(value: Long): Long = value

    @androidx.room.TypeConverter
    fun fromInt(value: Int): Int = value

    @androidx.room.TypeConverter
    fun toInt(value: Int): Int = value

    @androidx.room.TypeConverter
    fun fromBoolean(value: Boolean): Int = if (value) 1 else 0

    @androidx.room.TypeConverter
    fun toBoolean(value: Int): Boolean = value != 0
}
