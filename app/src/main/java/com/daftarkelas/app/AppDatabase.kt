package com.daftarkelas.app

import android.content.Context
import androidx.room.*

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val className: String,
    val parentPhone: String
)

@Entity(tableName = "class_sessions")
data class ClassSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String,
    val isVirtual: Boolean,
    val shamsiDate: String,
    val time: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val status: String, // "حاضر", "غایب", "تأخیر"
    val homeworkStatus: String // "کامل", "ناقص", "-"
)

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val reason: String, // "PRESENT_ABSENCE", "VIRTUAL_2_ABSENCE", "VIRTUAL_2_HOMEWORK"
    val lastSessionId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ClassDao {
    @Insert fun insertStudent(student: Student): Long
    @Update fun updateStudent(student: Student)
    @Delete fun deleteStudent(student: Student)
    @Query("SELECT * FROM students ORDER BY name ASC") fun getAllStudents(): List<Student>

    @Insert fun insertSession(session: ClassSession): Long
    @Query("SELECT * FROM class_sessions ORDER BY timestamp DESC") fun getAllSessions(): List<ClassSession>

    @Insert fun insertAttendanceRecords(records: List<AttendanceRecord>)
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId") fun getRecordsForSession(sessionId: Long): List<AttendanceRecord>
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId") fun getRecordsForStudent(studentId: Long): List<AttendanceRecord>

    @Insert fun insertSmsLog(log: SmsLog)
    @Query("SELECT * FROM sms_logs WHERE studentId = :studentId AND reason = :reason ORDER BY timestamp DESC LIMIT 1")
    fun getLastSmsLog(studentId: Long, reason: String): SmsLog?
}

@Database(entities = [Student::class, ClassSession::class, AttendanceRecord::class, SmsLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daftar_kelas_db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
