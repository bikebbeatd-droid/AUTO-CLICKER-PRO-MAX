package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.ClickMode
import com.example.data.model.ClickTarget
import com.example.data.model.MacroAction
import com.example.data.model.SequenceBlock
import com.example.data.model.SwipeConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val mode: String,
    val targetsJson: String,
    val swipeConfigJson: String?,
    val macroId: String?,
    val repeatCount: Int,
    val intervalMs: Long,
    val isPreset: Boolean
)

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val repeatCount: Int,
    val infiniteLoop: Boolean,
    val actionsJson: String,
    val createdAt: Long,
    val folderName: String
)

@Entity(tableName = "sequence_scripts")
data class SequenceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val blocksJson: String,
    val repeatCount: Int,
    val updatedAt: Long
)

@Entity(tableName = "session_stats")
data class SessionStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val clicks: Long,
    val successfulGestures: Long,
    val failedGestures: Long,
    val durationSeconds: Long,
    val averageIntervalMs: Long,
    val mode: String,
    val timestamp: Long
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val profileId: String,
    val scheduleType: String,
    val triggerHour: Int,
    val triggerMinute: Int,
    val sessionDurationMinutes: Int,
    val isEnabled: Boolean
)

class DatabaseConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val targetListType = Types.newParameterizedType(List::class.java, ClickTarget::class.java)
    private val targetListAdapter = moshi.adapter<List<ClickTarget>>(targetListType)

    private val actionListType = Types.newParameterizedType(List::class.java, MacroAction::class.java)
    private val actionListAdapter = moshi.adapter<List<MacroAction>>(actionListType)

    private val blockListType = Types.newParameterizedType(List::class.java, SequenceBlock::class.java)
    private val blockListAdapter = moshi.adapter<List<SequenceBlock>>(blockListType)

    private val swipeConfigAdapter = moshi.adapter(SwipeConfig::class.java)

    @TypeConverter
    fun fromTargetList(targets: List<ClickTarget>?): String {
        return targets?.let { targetListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toTargetList(json: String?): List<ClickTarget> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { targetListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    @TypeConverter
    fun fromActionList(actions: List<MacroAction>?): String {
        return actions?.let { actionListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toActionList(json: String?): List<MacroAction> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { actionListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    @TypeConverter
    fun fromBlockList(blocks: List<SequenceBlock>?): String {
        return blocks?.let { blockListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toBlockList(json: String?): List<SequenceBlock> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { blockListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    @TypeConverter
    fun fromSwipeConfig(config: SwipeConfig?): String? {
        return config?.let { swipeConfigAdapter.toJson(it) }
    }

    @TypeConverter
    fun toSwipeConfig(json: String?): SwipeConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching { swipeConfigAdapter.fromJson(json) }.getOrNull()
    }
}
