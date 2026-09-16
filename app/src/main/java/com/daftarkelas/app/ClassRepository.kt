package com.daftarkelas.app

import android.content.Context
import android.content.Intent
import android.net.Uri

class ClassRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).classDao()

    fun checkAndGetSmsAlerts(sessionId: Long, isVirtual: Boolean, shamsiDate: String): List<Pair<String, String>> {
        val smsList = mutableListOf<Pair<String, String>>()
        val sessionRecords = dao.getRecordsForSession(sessionId)
        val students = dao.getAllStudents().associateBy { it.id }

        for (record in sessionRecords) {
            val student = students[record.studentId] ?: continue
            if (student.parentPhone.isBlank()) continue

            if (!isVirtual) {
                // حضوری: تک جلسه غیبت
                if (record.status == "غایب") {
                    val msg = "ولی گرامی، فرزند شما ${student.name} در تاریخ $shamsiDate در کلاس حضور نداشته است."
                    smsList.add(Pair(student.parentPhone, msg))
                }
            } else {
                // مجازی: منطق ۳ جلسه متوالی
                val studentRecords = dao.getRecordsForStudent(student.id).sortedBy { it.sessionId }
                if (studentRecords.size >= 3) {
                    val lastThree = studentRecords.takeLast(3)
                    
                    // ۳ غیبت متوالی
                    if (lastThree.all { it.status == "غایب" }) {
                        val lastLog = dao.getLastSmsLog(student.id, "VIRTUAL_3_ABSENCE")
                        if (lastLog == null || lastLog.lastSessionId != sessionId - 1) {
                            val msg = "ولی گرامی، فرزند شما ${student.name} در سه جلسه متوالی کلاس مجازی غیبت داشته است. لطفاً موضوع را پیگیری فرمایید."
                            smsList.add(Pair(student.parentPhone, msg))
                            dao.insertSmsLog(SmsLog(studentId = student.id, reason = "VIRTUAL_3_ABSENCE", lastSessionId = sessionId))
                        }
                    }

                    // ۳ تکلیف ناقص متوالی
                    if (lastThree.all { it.homeworkStatus == "ناقص" }) {
                        val lastLog = dao.getLastSmsLog(student.id, "VIRTUAL_3_HOMEWORK")
                        if (lastLog == null || lastLog.lastSessionId != sessionId - 1) {
                            val msg = "ولی گرامی، تکالیف فرزند شما ${student.name} در سه جلسه متوالی کلاس مجازی ناقص بوده است. لطفاً موضوع را پیگیری فرمایید."
                            smsList.add(Pair(student.parentPhone, msg))
                            dao.insertSmsLog(SmsLog(studentId = student.id, reason = "VIRTUAL_3_HOMEWORK", lastSessionId = sessionId))
                        }
                    }
                }
            }
        }
        return smsList
    }

    fun sendSmsIntent(context: Context, phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
