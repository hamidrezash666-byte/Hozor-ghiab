package com.daftarkelas.app

import android.content.Context
import android.content.Intent
import android.net.Uri

class ClassRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).classDao()

    fun checkAndGetSmsAlerts(sessionId: Long, isVirtual: Boolean, dateStr: String): List<Pair<String, String>> {
        val alerts = mutableListOf<Pair<String, String>>()
        val currentRecords = dao.getRecordsForSession(sessionId)
        val students = dao.getAllStudents().associateBy { it.id }

        for (rec in currentRecords) {
            val student = students[rec.studentId] ?: continue
            if (student.parentPhone.isBlank()) continue

            if (isVirtual) {
                // هشدار ۳ جلسه متوالی غیبت در کلاس مجازی
                if (rec.status == "غایب") {
                    val allStudentRecords = dao.getRecordsForStudent(student.id)
                    val sortedVirtualRecords = allStudentRecords.filter { r ->
                        val s = dao.getAllSessions().find { it.id == r.sessionId }
                        s?.isVirtual == true
                    }.sortedByDescending { it.sessionId }

                    if (sortedVirtualRecords.size >= 3) {
                        val last3 = sortedVirtualRecords.take(3)
                        if (last3.all { it.status == "غایب" }) {
                            val lastSms = dao.getLastSmsLog(student.id, "3_ABSENT_VIRTUAL")
                            val alreadySent = lastSms != null && lastSms.lastSessionId == sessionId

                            if (!alreadySent) {
                                val msg = "ولی گرامی،\nبه اطلاع می‌رساند دانش‌آموز ${student.name} در ۳ جلسه متوالی اخیر کلاس مجازی (از جمله $dateStr) حضور نداشته است.\nبا تشکر"
                                alerts.add(Pair(student.parentPhone, msg))
                                dao.insertSmsLog(SmsLog(studentId = student.id, reason = "3_ABSENT_VIRTUAL", lastSessionId = sessionId))
                            }
                        }
                    }
                }

                // هشدار ۳ جلسه متوالی تکلیف ناقص در کلاس مجازی
                if (rec.status == "حاضر" && rec.homeworkStatus == "ناقص") {
                    val allStudentRecords = dao.getRecordsForStudent(student.id)
                    val sortedVirtualRecords = allStudentRecords.filter { r ->
                        val s = dao.getAllSessions().find { it.id == r.sessionId }
                        s?.isVirtual == true
                    }.sortedByDescending { it.sessionId }

                    if (sortedVirtualRecords.size >= 3) {
                        val last3 = sortedVirtualRecords.take(3)
                        if (last3.all { it.status == "حاضر" && it.homeworkStatus == "ناقص" }) {
                            val lastSms = dao.getLastSmsLog(student.id, "3_HW_INCOMPLETE")
                            val alreadySent = lastSms != null && lastSms.lastSessionId == sessionId

                            if (!alreadySent) {
                                val msg = "ولی گرامی،\nبه اطلاع می‌رساند تکالیف درسی دانش‌آموز ${student.name} در ۳ جلسه متوالی اخیر کلاس مجازی (از جمله $dateStr) به‌صورت ناقص ارائه شده است. لطفا جهت پیشرفت تحصیلی ایشان، نظارت لازم را داشته باشید.\nبا تشکر"
                                alerts.add(Pair(student.parentPhone, msg))
                                dao.insertSmsLog(SmsLog(studentId = student.id, reason = "3_HW_INCOMPLETE", lastSessionId = sessionId))
                            }
                        }
                    }
                }
            }
        }
        return alerts
    }

    fun sendSmsIntent(context: Context, phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
