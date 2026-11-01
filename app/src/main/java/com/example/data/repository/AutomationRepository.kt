package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.DatabaseConverters
import com.example.data.db.MacroEntity
import com.example.data.db.ProfileEntity
import com.example.data.db.ScheduledTaskEntity
import com.example.data.db.SequenceEntity
import com.example.data.db.SessionStatsEntity
import com.example.data.model.AutomationProfile
import com.example.data.model.AutomationStats
import com.example.data.model.ClickMode
import com.example.data.model.ClickTarget
import com.example.data.model.Macro
import com.example.data.model.MacroAction
import com.example.data.model.ScheduledTask
import com.example.data.model.SequenceBlock
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class AutomationRepository(private val db: AppDatabase) {
    private val converters = DatabaseConverters()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val macroAdapter = moshi.adapter(Macro::class.java)
    private val profileAdapter = moshi.adapter(AutomationProfile::class.java)

    // Profiles
    val allProfiles: Flow<List<AutomationProfile>> = db.profileDao().getAllProfiles().map { list ->
        list.map { entityToProfile(it) }
    }

    suspend fun getProfile(id: String): AutomationProfile? {
        val entity = db.profileDao().getProfileById(id) ?: return null
        return entityToProfile(entity)
    }

    suspend fun saveProfile(profile: AutomationProfile) {
        db.profileDao().insertProfile(profileToEntity(profile))
    }

    suspend fun deleteProfile(id: String) {
        db.profileDao().deleteProfile(id)
    }

    suspend fun duplicateProfile(profile: AutomationProfile): AutomationProfile {
        val cloned = profile.copy(
            id = UUID.randomUUID().toString(),
            name = "${profile.name} (Copy)",
            isPreset = false
        )
        saveProfile(cloned)
        return cloned
    }

    // Macros
    val allMacros: Flow<List<Macro>> = db.macroDao().getAllMacros().map { list ->
        list.map { entityToMacro(it) }
    }

    suspend fun getMacro(id: String): Macro? {
        val entity = db.macroDao().getMacroById(id) ?: return null
        return entityToMacro(entity)
    }

    suspend fun saveMacro(macro: Macro) {
        db.macroDao().insertMacro(macroToEntity(macro))
    }

    suspend fun deleteMacro(id: String) {
        db.macroDao().deleteMacro(id)
    }

    fun exportMacroToJson(macro: Macro): String {
        return macroAdapter.indent("  ").toJson(macro)
    }

    fun importMacroFromJson(json: String): Result<Macro> {
        return runCatching {
            val macro = macroAdapter.fromJson(json) ?: throw IllegalArgumentException("Invalid macro JSON format")
            macro.copy(id = UUID.randomUUID().toString(), name = "${macro.name} (Imported)")
        }
    }

    fun exportProfileToJson(profile: AutomationProfile): String {
        return profileAdapter.indent("  ").toJson(profile)
    }

    fun importProfileFromJson(json: String): Result<AutomationProfile> {
        return runCatching {
            val profile = profileAdapter.fromJson(json) ?: throw IllegalArgumentException("Invalid profile JSON format")
            profile.copy(id = UUID.randomUUID().toString(), name = "${profile.name} (Imported)", isPreset = false)
        }
    }

    // Sequences
    val allSequences: Flow<List<SequenceBlockList>> = db.sequenceDao().getAllSequences().map { list ->
        list.map {
            SequenceBlockList(
                id = it.id,
                name = it.name,
                description = it.description,
                blocks = converters.toBlockList(it.blocksJson),
                repeatCount = it.repeatCount
            )
        }
    }

    suspend fun saveSequence(id: String, name: String, description: String, blocks: List<SequenceBlock>, repeatCount: Int = 1) {
        db.sequenceDao().insertSequence(
            SequenceEntity(
                id = id,
                name = name,
                description = description,
                blocksJson = converters.fromBlockList(blocks),
                repeatCount = repeatCount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getSequence(id: String): SequenceBlockList? {
        val entity = db.sequenceDao().getSequenceById(id) ?: return null
        return SequenceBlockList(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            blocks = converters.toBlockList(entity.blocksJson),
            repeatCount = entity.repeatCount
        )
    }

    suspend fun deleteSequence(id: String) {
        db.sequenceDao().deleteSequence(id)
    }

    // Statistics
    val sessionStats: Flow<List<SessionStatsEntity>> = db.statsDao().getAllStats()

    val totalStatsSummary: Flow<AutomationStats> = combine(
        db.statsDao().getTotalClicks(),
        db.statsDao().getTotalSuccessfulGestures(),
        db.statsDao().getTotalSessionsCount()
    ) { clicks, successful, sessions ->
        AutomationStats(
            totalClicks = clicks ?: 0L,
            successfulGestures = successful ?: 0L,
            failedGestures = maxOf(0L, (clicks ?: 0L) - (successful ?: 0L)),
            totalSessions = sessions
        )
    }

    suspend fun recordSessionStats(
        clicks: Long,
        successful: Long,
        failed: Long,
        durationSeconds: Long,
        averageInterval: Long,
        mode: String
    ) {
        if (clicks > 0 || durationSeconds > 0) {
            db.statsDao().insertSessionStats(
                SessionStatsEntity(
                    clicks = clicks,
                    successfulGestures = successful,
                    failedGestures = failed,
                    durationSeconds = durationSeconds,
                    averageIntervalMs = averageInterval,
                    mode = mode,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Scheduled Tasks
    val scheduledTasks: Flow<List<ScheduledTask>> = db.scheduledTaskDao().getAllScheduledTasks().map { list ->
        list.map {
            ScheduledTask(
                id = it.id,
                name = it.name,
                profileId = it.profileId,
                scheduleType = it.scheduleType,
                triggerHour = it.triggerHour,
                triggerMinute = it.triggerMinute,
                sessionDurationMinutes = it.sessionDurationMinutes,
                isEnabled = it.isEnabled
            )
        }
    }

    suspend fun saveScheduledTask(task: ScheduledTask) {
        db.scheduledTaskDao().insertScheduledTask(
            ScheduledTaskEntity(
                id = task.id,
                name = task.name,
                profileId = task.profileId,
                scheduleType = task.scheduleType,
                triggerHour = task.triggerHour,
                triggerMinute = task.triggerMinute,
                sessionDurationMinutes = task.sessionDurationMinutes,
                isEnabled = task.isEnabled
            )
        )
    }

    suspend fun deleteScheduledTask(id: String) {
        db.scheduledTaskDao().deleteScheduledTask(id)
    }

    suspend fun toggleScheduledTask(id: String, enabled: Boolean) {
        db.scheduledTaskDao().setTaskEnabled(id, enabled)
    }

    // Entity converters
    private fun entityToProfile(entity: ProfileEntity): AutomationProfile {
        return AutomationProfile(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            mode = runCatching { ClickMode.valueOf(entity.mode) }.getOrDefault(ClickMode.SINGLE_TAP),
            targets = converters.toTargetList(entity.targetsJson),
            swipeConfig = converters.toSwipeConfig(entity.swipeConfigJson),
            macroId = entity.macroId,
            repeatCount = entity.repeatCount,
            intervalMs = entity.intervalMs,
            isPreset = entity.isPreset
        )
    }

    private fun profileToEntity(profile: AutomationProfile): ProfileEntity {
        return ProfileEntity(
            id = profile.id,
            name = profile.name,
            description = profile.description,
            mode = profile.mode.name,
            targetsJson = converters.fromTargetList(profile.targets),
            swipeConfigJson = converters.fromSwipeConfig(profile.swipeConfig),
            macroId = profile.macroId,
            repeatCount = profile.repeatCount,
            intervalMs = profile.intervalMs,
            isPreset = profile.isPreset
        )
    }

    private fun entityToMacro(entity: MacroEntity): Macro {
        return Macro(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            repeatCount = entity.repeatCount,
            infiniteLoop = entity.infiniteLoop,
            actions = converters.toActionList(entity.actionsJson),
            createdAt = entity.createdAt,
            folderName = entity.folderName
        )
    }

    private fun macroToEntity(macro: Macro): MacroEntity {
        return MacroEntity(
            id = macro.id,
            name = macro.name,
            description = macro.description,
            repeatCount = macro.repeatCount,
            infiniteLoop = macro.infiniteLoop,
            actionsJson = converters.fromActionList(macro.actions),
            createdAt = macro.createdAt,
            folderName = macro.folderName
        )
    }
}

data class SequenceBlockList(
    val id: String,
    val name: String,
    val description: String,
    val blocks: List<SequenceBlock>,
    val repeatCount: Int = 1
)
