package com.localaipainter.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localaipainter.data.models.*

/**
 * Room 数据库 — 6 实体 + 6 DAO + 3 迁移
 * v1: 初始版本
 * v2: 添加 LoRA / Task 表
 * v3: 添加 GenerationConfig 字段扩展
 */
@Database(
    entities = [
        ModelEntity::class,
        HistoryEntity::class,
        GenerationConfig::class,
        LoRAEntity::class,
        TaskEntity::class,
        GenerationHistory::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    // ─── DAOs ───
    abstract fun modelDao(): ModelDao
    abstract fun historyDao(): HistoryDao
    abstract fun generationConfigDao(): GenerationConfigDao
    abstract fun loraDao(): LoRADao
    abstract fun taskDao(): TaskDao
    abstract fun generationHistoryDao(): GenerationHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localaipainter.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // ─── Migration v1 → v2 ───
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 LoRA 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `lora_models` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `path` TEXT NOT NULL,
                        `type` TEXT NOT NULL DEFAULT 'lora',
                        `rank` INTEGER NOT NULL DEFAULT 4,
                        `alpha` REAL NOT NULL DEFAULT 1.0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `size_mb` REAL NOT NULL DEFAULT 0,
                        `imported_at` INTEGER NOT NULL DEFAULT 0,
                        `trigger_words` TEXT NOT NULL DEFAULT ''
                    )
                """)
                // 创建 Task 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'pending',
                        `progress` REAL NOT NULL DEFAULT 0,
                        `params_json` TEXT NOT NULL DEFAULT '{}',
                        `result_path` TEXT,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        `error_msg` TEXT
                    )
                """)
                // GenerationConfig 添加字段
                db.execSQL("ALTER TABLE `generation_configs` ADD COLUMN `sampler` TEXT NOT NULL DEFAULT 'euler_a'")
                db.execSQL("ALTER TABLE `generation_configs` ADD COLUMN `seed` INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE `generation_configs` ADD COLUMN `controlnet_type` TEXT NOT NULL DEFAULT ''")
            }
        }

        // ─── Migration v2 → v3 ───
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 GenerationHistory 表（v3 新增，与 HistoryEntity 分开）
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `generation_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `negative_prompt` TEXT NOT NULL DEFAULT '',
                        `width` INTEGER NOT NULL DEFAULT 512,
                        `height` INTEGER NOT NULL DEFAULT 512,
                        `steps` INTEGER NOT NULL DEFAULT 20,
                        `cfg_scale` REAL NOT NULL DEFAULT 7.5,
                        `seed` INTEGER NOT NULL DEFAULT -1,
                        `sampler` TEXT NOT NULL DEFAULT 'euler_a',
                        `model_name` TEXT NOT NULL DEFAULT '',
                        `lora_names` TEXT NOT NULL DEFAULT '',
                        `image_path` TEXT,
                        `thumb_path` TEXT,
                        `latency_ms` REAL NOT NULL DEFAULT 0,
                        `peak_memory_mb` REAL NOT NULL DEFAULT 0,
                        `backend_used` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `favorite` INTEGER NOT NULL DEFAULT 0,
                        `tags` TEXT NOT NULL DEFAULT ''
                    )
                """)
                // ModelEntity 添加字段
                db.execSQL("ALTER TABLE `models` ADD COLUMN `format` TEXT NOT NULL DEFAULT 'safetensors'")
                db.execSQL("ALTER TABLE `models` ADD COLUMN `quantization` TEXT NOT NULL DEFAULT 'fp16'")
                db.execSQL("ALTER TABLE `models` ADD COLUMN `architecture` TEXT NOT NULL DEFAULT 'sd15'")
                db.execSQL("ALTER TABLE `models` ADD COLUMN `supports_int8` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `models` ADD COLUMN `supports_int4` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

// ─═════════════════════════════════════════════════════════════════
//  Entities
// ─═════════════════════════════════════════════════════════════════

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "size_mb") val sizeMb: Float,
    @ColumnInfo(name = "imported_at") val importedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "type") val type: String = "checkpoint",  // checkpoint / vae / controlnet / textual_inversion
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "format") val format: String = "safetensors", // safetensors / gguf / onnx / mnn
    @ColumnInfo(name = "quantization") val quantization: String = "fp16", // fp32 / fp16 / int8 / int4 / int2
    @ColumnInfo(name = "architecture") val architecture: String = "sd15", // sd15 / sd21 / sdxl / lcm / turbo
    @ColumnInfo(name = "supports_int8") val supportsInt8: Boolean = true,
    @ColumnInfo(name = "supports_int4") val supportsInt4: Boolean = false,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "prompt") val prompt: String,
    @ColumnInfo(name = "negative_prompt") val negativePrompt: String = "",
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "thumb_path") val thumbPath: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "params_json") val paramsJson: String = "{}",
    @ColumnInfo(name = "model_used") val modelUsed: String = "",
    @ColumnInfo(name = "latency_ms") val latencyMs: Float = 0f,
    @ColumnInfo(name = "favorite") val favorite: Boolean = false,
)

@Entity(tableName = "generation_configs")
data class GenerationConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String = "Default",
    @ColumnInfo(name = "width") val width: Int = 512,
    @ColumnInfo(name = "height") val height: Int = 512,
    @ColumnInfo(name = "steps") val steps: Int = 20,
    @ColumnInfo(name = "cfg_scale") val cfgScale: Float = 7.5f,
    @ColumnInfo(name = "sampler") val sampler: String = "euler_a",
    @ColumnInfo(name = "seed") val seed: Int = -1,
    @ColumnInfo(name = "controlnet_type") val controlNetType: String = "",
    @ColumnInfo(name = "model_path") val modelPath: String = "",
    @ColumnInfo(name = "lora_config_json") val loraConfigJson: String = "{}",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "lora_models")
data class LoRAEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "type") val type: String = "lora",
    @ColumnInfo(name = "rank") val rank: Int = 4,
    @ColumnInfo(name = "alpha") val alpha: Float = 1.0f,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "size_mb") val sizeMb: Float = 0f,
    @ColumnInfo(name = "imported_at") val importedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "trigger_words") val triggerWords: String = "",
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,  // generate / import / download / train
    @ColumnInfo(name = "status") val status: String = "pending", // pending / running / done / failed / cancelled
    @ColumnInfo(name = "progress") val progress: Float = 0f,
    @ColumnInfo(name = "params_json") val paramsJson: String = "{}",
    @ColumnInfo(name = "result_path") val resultPath: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "error_msg") val errorMsg: String? = null,
)

@Entity(tableName = "generation_history")
data class GenerationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "prompt") val prompt: String,
    @ColumnInfo(name = "negative_prompt") val negativePrompt: String = "",
    @ColumnInfo(name = "width") val width: Int = 512,
    @ColumnInfo(name = "height") val height: Int = 512,
    @ColumnInfo(name = "steps") val steps: Int = 20,
    @ColumnInfo(name = "cfg_scale") val cfgScale: Float = 7.5f,
    @ColumnInfo(name = "seed") val seed: Int = -1,
    @ColumnInfo(name = "sampler") val sampler: String = "euler_a",
    @ColumnInfo(name = "model_name") val modelName: String = "",
    @ColumnInfo(name = "lora_names") val loraNames: String = "",
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "thumb_path") val thumbPath: String? = null,
    @ColumnInfo(name = "latency_ms") val latencyMs: Float = 0f,
    @ColumnInfo(name = "peak_memory_mb") val peakMemoryMb: Float = 0f,
    @ColumnInfo(name = "backend_used") val backendUsed: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "favorite") val favorite: Boolean = false,
    @ColumnInfo(name = "tags") val tags: String = "",
)

// ─═════════════════════════════════════════════════════════════════
//  DAOs
// ─═════════════════════════════════════════════════════════════════

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY imported_at DESC")
    fun getAll(): List<ModelEntity>

    @Query("SELECT * FROM models WHERE type = :type ORDER BY name ASC")
    fun getByType(type: String): List<ModelEntity>

    @Query("SELECT * FROM models WHERE id = :id")
    fun getById(id: Long): ModelEntity?

    @Query("SELECT * FROM models WHERE enabled = 1 AND supports_int8 = 1")
    fun getInt8Capable(): List<ModelEntity>

    @Insert
    fun insert(model: ModelEntity): Long

    @Update
    fun update(model: ModelEntity)

    @Delete
    fun delete(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): List<HistoryEntity>

    @Insert
    fun insert(history: HistoryEntity): Long

    @Update
    fun update(history: HistoryEntity)

    @Delete
    fun delete(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface GenerationConfigDao {
    @Query("SELECT * FROM generation_configs ORDER BY is_default DESC, name ASC")
    fun getAll(): List<GenerationConfig>

    @Query("SELECT * FROM generation_configs WHERE is_default = 1 LIMIT 1")
    fun getDefault(): GenerationConfig?

    @Insert
    fun insert(config: GenerationConfig): Long

    @Update
    fun update(config: GenerationConfig)

    @Delete
    fun delete(config: GenerationConfig)
}

@Dao
interface LoRADao {
    @Query("SELECT * FROM lora_models ORDER BY name ASC")
    fun getAll(): List<LoRAEntity>

    @Query("SELECT * FROM lora_models WHERE enabled = 1")
    fun getEnabled(): List<LoRAEntity>

    @Insert
    fun insert(lora: LoRAEntity): Long

    @Update
    fun update(lora: LoRAEntity)

    @Delete
    fun delete(lora: LoRAEntity)

    @Query("DELETE FROM lora_models WHERE id = :id")
    fun deleteById(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status != 'done' AND status != 'failed' ORDER BY created_at ASC")
    fun getActive(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY updated_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): List<TaskEntity>

    @Insert
    fun insert(task: TaskEntity): Long

    @Update
    fun update(task: TaskEntity)

    @Delete
    fun delete(task: TaskEntity)
}

@Dao
interface GenerationHistoryDao {
    @Query("SELECT * FROM generation_history ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): List<GenerationHistory>

    @Query("SELECT * FROM generation_history WHERE favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): List<GenerationHistory>

    @Query("SELECT * FROM generation_history WHERE prompt LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByPrompt(query: String): List<GenerationHistory>

    @Insert
    fun insert(entry: GenerationHistory): Long

    @Update
    fun update(entry: GenerationHistory)

    @Query("DELETE FROM generation_history WHERE id = :id")
    fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM generation_history")
    fun count(): Int
}
