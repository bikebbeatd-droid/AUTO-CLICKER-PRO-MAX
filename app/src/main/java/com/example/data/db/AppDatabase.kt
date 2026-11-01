package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isPreset DESC, name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY createdAt DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getMacroById(id: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacro(id: String)
}

@Dao
interface SequenceDao {
    @Query("SELECT * FROM sequence_scripts ORDER BY updatedAt DESC")
    fun getAllSequences(): Flow<List<SequenceEntity>>

    @Query("SELECT * FROM sequence_scripts WHERE id = :id")
    suspend fun getSequenceById(id: String): SequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: SequenceEntity)

    @Query("DELETE FROM sequence_scripts WHERE id = :id")
    suspend fun deleteSequence(id: String)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM session_stats ORDER BY timestamp DESC LIMIT 50")
    fun getAllStats(): Flow<List<SessionStatsEntity>>

    @Query("SELECT SUM(clicks) FROM session_stats")
    fun getTotalClicks(): Flow<Long?>

    @Query("SELECT SUM(successfulGestures) FROM session_stats")
    fun getTotalSuccessfulGestures(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM session_stats")
    fun getTotalSessionsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionStats(stats: SessionStatsEntity)
}

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY triggerHour ASC, triggerMinute ASC")
    fun getAllScheduledTasks(): Flow<List<ScheduledTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTask(task: ScheduledTaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteScheduledTask(id: String)

    @Query("UPDATE scheduled_tasks SET isEnabled = :enabled WHERE id = :id")
    suspend fun setTaskEnabled(id: String, enabled: Boolean)
}

@Database(
    entities = [
        ProfileEntity::class,
        MacroEntity::class,
        SequenceEntity::class,
        SessionStatsEntity::class,
        ScheduledTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun macroDao(): MacroDao
    abstract fun sequenceDao(): SequenceDao
    abstract fun statsDao(): StatsDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoclicker_pro_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDefaultProfiles(getDatabase(context).profileDao(), getDatabase(context).macroDao())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultProfiles(profileDao: ProfileDao, macroDao: MacroDao) {
            val converters = DatabaseConverters()
            
            // 1. Basic Auto Tap
            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_single_tap",
                    name = "Basic Auto Tap",
                    description = "Rapid single-point clicker for games & repeated button tapping",
                    mode = "SINGLE_TAP",
                    targetsJson = converters.fromTargetList(
                        listOf(
                            com.example.data.model.ClickTarget(
                                id = "target_1",
                                number = 1,
                                x = 540f,
                                y = 960f,
                                intervalMs = 100L,
                                durationMs = 40L,
                                colorHex = "#00F0FF",
                                label = "Primary Tap"
                            )
                        )
                    ),
                    swipeConfigJson = null,
                    macroId = null,
                    repeatCount = 0,
                    intervalMs = 100L,
                    isPreset = true
                )
            )

            // 2. Multi-Point Tap
            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_multi_point",
                    name = "Multi-Point Tap",
                    description = "Cycles sequentially between 3 customizable target coordinates",
                    mode = "MULTI_TAP",
                    targetsJson = converters.fromTargetList(
                        listOf(
                            com.example.data.model.ClickTarget(
                                id = "target_m1",
                                number = 1,
                                x = 320f,
                                y = 800f,
                                intervalMs = 150L,
                                durationMs = 45L,
                                colorHex = "#00F0FF",
                                label = "Top Left"
                            ),
                            com.example.data.model.ClickTarget(
                                id = "target_m2",
                                number = 2,
                                x = 760f,
                                y = 800f,
                                intervalMs = 150L,
                                durationMs = 45L,
                                colorHex = "#A855F7",
                                label = "Top Right"
                            ),
                            com.example.data.model.ClickTarget(
                                id = "target_m3",
                                number = 3,
                                x = 540f,
                                y = 1200f,
                                intervalMs = 200L,
                                durationMs = 50L,
                                colorHex = "#EC4899",
                                label = "Bottom Center"
                            )
                        )
                    ),
                    swipeConfigJson = null,
                    macroId = null,
                    repeatCount = 0,
                    intervalMs = 150L,
                    isPreset = true
                )
            )

            // 3. Auto Scroll
            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_auto_scroll",
                    name = "Auto Scroll",
                    description = "Continuous smooth upward scroll for reading news feeds & social posts",
                    mode = "AUTO_SWIPE",
                    targetsJson = "[]",
                    swipeConfigJson = converters.fromSwipeConfig(
                        com.example.data.model.SwipeConfig(
                            startX = 540f,
                            startY = 1400f,
                            endX = 540f,
                            endY = 500f,
                            durationMs = 400L,
                            delayAfterMs = 1200L,
                            swipeDirection = "UP"
                        )
                    ),
                    macroId = null,
                    repeatCount = 0,
                    intervalMs = 1200L,
                    isPreset = true
                )
            )

            // 4. Long Press
            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_long_press",
                    name = "Long Press",
                    description = "Continuous long press gesture with 1500ms hold duration",
                    mode = "SINGLE_TAP",
                    targetsJson = converters.fromTargetList(
                        listOf(
                            com.example.data.model.ClickTarget(
                                id = "target_lp",
                                number = 1,
                                x = 540f,
                                y = 960f,
                                intervalMs = 500L,
                                durationMs = 1500L,
                                colorHex = "#F59E0B",
                                label = "Long Press Point"
                            )
                        )
                    ),
                    swipeConfigJson = null,
                    macroId = null,
                    repeatCount = 10,
                    intervalMs = 500L,
                    isPreset = true
                )
            )

            // 5. Repeated Swipe
            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_repeated_swipe",
                    name = "Repeated Swipe",
                    description = "Horizontal swipe gesture for page turning and card flipping",
                    mode = "AUTO_SWIPE",
                    targetsJson = "[]",
                    swipeConfigJson = converters.fromSwipeConfig(
                        com.example.data.model.SwipeConfig(
                            startX = 900f,
                            startY = 1000f,
                            endX = 180f,
                            endY = 1000f,
                            durationMs = 250L,
                            delayAfterMs = 800L,
                            swipeDirection = "LEFT"
                        )
                    ),
                    macroId = null,
                    repeatCount = 0,
                    intervalMs = 800L,
                    isPreset = true
                )
            )

            // 6. Custom Macro preset
            val macroId = "macro_demo_combo"
            macroDao.insertMacro(
                MacroEntity(
                    id = macroId,
                    name = "Combo Tap & Swipe",
                    description = "Executes two quick taps followed by an upward swipe",
                    repeatCount = 3,
                    infiniteLoop = false,
                    actionsJson = converters.fromActionList(
                        listOf(
                            com.example.data.model.MacroAction(
                                id = "act_1",
                                type = com.example.data.model.GestureType.TAP,
                                x = 400f,
                                y = 800f,
                                durationMs = 50L,
                                delayAfterMs = 200L,
                                orderIndex = 0
                            ),
                            com.example.data.model.MacroAction(
                                id = "act_2",
                                type = com.example.data.model.GestureType.TAP,
                                x = 680f,
                                y = 800f,
                                durationMs = 50L,
                                delayAfterMs = 300L,
                                orderIndex = 1
                            ),
                            com.example.data.model.MacroAction(
                                id = "act_3",
                                type = com.example.data.model.GestureType.SWIPE,
                                x = 540f,
                                y = 1300f,
                                endX = 540f,
                                endY = 600f,
                                durationMs = 350L,
                                delayAfterMs = 500L,
                                orderIndex = 2
                            )
                        )
                    ),
                    createdAt = System.currentTimeMillis(),
                    folderName = "Presets"
                )
            )

            profileDao.insertProfile(
                ProfileEntity(
                    id = "preset_macro_combo",
                    name = "Custom Macro",
                    description = "Recorded sequence with combo taps and timed swipes",
                    mode = "MACRO_RECORDER",
                    targetsJson = "[]",
                    swipeConfigJson = null,
                    macroId = macroId,
                    repeatCount = 5,
                    intervalMs = 500L,
                    isPreset = true
                )
            )
        }
    }
}
