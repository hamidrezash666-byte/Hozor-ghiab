package com.daftarkelas.app

import android.content.Context
import androidx.room.*

// جدول تعریف برنامه هفتگی کلاس‌ها
@Entity(tableName = "weekly_schedule")
data class WeeklySchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1: شنبه, 2: یکشنبه, ..., 7: جمعه
    val subjectName: String,
    val className: String,
    val timeSlot: String
)

// جدول طرح درس‌های ثبت‌شده برای هر جلسه
@Entity(tableName = "lesson_plans")
data class LessonPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val dateStr: String, // تاریخ شمسی (مثلاً 1405/06/25)
    val topic: String = "",
    val objective: String = "",
    val teachingMethod: String = "",
    val learningActivity: String = "",
    val noteForNextSession: String = ""
)

@Dao
interface LessonPlanDao {
    // برنامه هفتگی
    @Insert fun insertSchedule(schedule: WeeklySchedule): Long
    @Query("SELECT * FROM weekly_schedule ORDER BY dayOfWeek ASC, timeSlot ASC") fun getAllSchedules(): List<WeeklySchedule>
    @Query("SELECT * FROM weekly_schedule WHERE dayOfWeek = :dayOfWeek") fun getSchedulesForDay(dayOfWeek: Int): List<WeeklySchedule>
    @Delete fun deleteSchedule(schedule: WeeklySchedule)

    // طرح درس
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertOrUpdatePlan(plan: LessonPlan): Long
    @Query("SELECT * FROM lesson_plans WHERE scheduleId = :scheduleId AND dateStr = :dateStr LIMIT 1")
    fun getPlanForDate(scheduleId: Long, dateStr: String): LessonPlan?

    // دریافت آخرین یادداشت ثبت‌شده برای این درس بر اساس نام درس در تمام روزهای هفته
    @Query("""
        SELECT lp.noteForNextSession FROM lesson_plans lp
        JOIN weekly_schedule ws ON lp.scheduleId = ws.id
        JOIN weekly_schedule current_ws ON current_ws.id = :scheduleId
        WHERE ws.subjectName = current_ws.subjectName 
          AND lp.dateStr != :currentDate 
          AND lp.noteForNextSession != '' 
        ORDER BY lp.id DESC LIMIT 1
    """)
    fun getPreviousNote(scheduleId: Long, currentDate: String): String?
}

@Database(entities = [WeeklySchedule::class, LessonPlan::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonPlanDao(): LessonPlanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tarh_dars_db"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
