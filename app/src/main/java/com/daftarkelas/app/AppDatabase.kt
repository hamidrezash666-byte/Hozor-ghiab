package com.daftarkelas.app

import android.content.Context
import androidx.room.*

// جدول تعریف برنامه هفتگی کلاس‌ها
@Entity(tableName = "weekly_schedule")
data class WeeklySchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val selectedDays: String, // مثلاً "1,3,5" برای شنبه، دوشنبه، چهارشنبه
    val className: String = "",
    val timeSlot: String = ""
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
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertSchedule(schedule: WeeklySchedule): Long
    @Query("SELECT * FROM weekly_schedule ORDER BY id DESC") fun getAllSchedules(): List<WeeklySchedule>
    @Delete fun deleteSchedule(schedule: WeeklySchedule)

    // پیدا کردن درس‌هایی که در روز مورد نظر برگزار می‌شوند
    @Query("SELECT * FROM weekly_schedule WHERE selectedDays LIKE '%' || :dayOfWeek || '%'")
    fun getSchedulesForDay(dayOfWeek: Int): List<WeeklySchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertOrUpdatePlan(plan: LessonPlan): Long
    
    @Query("SELECT * FROM lesson_plans WHERE scheduleId = :scheduleId AND dateStr = :dateStr LIMIT 1")
    fun getPlanForDate(scheduleId: Long, dateStr: String): LessonPlan?

    // دریافت آخرین یادداشت ثبت‌شده برای این درس در تمام جلسات قبلی
    @Query("""
        SELECT noteForNextSession FROM lesson_plans 
        WHERE scheduleId = :scheduleId AND dateStr != :currentDate AND noteForNextSession != '' 
        ORDER BY id DESC LIMIT 1
    """)
    fun getPreviousNote(scheduleId: Long, currentDate: String): String?
}

@Database(entities = [WeeklySchedule::class, LessonPlan::class], version = 4, exportSchema = false)
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
