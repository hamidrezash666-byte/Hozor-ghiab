package com.daftarkelas.app

import java.util.Calendar

object ShamsiCalendar {

    fun getCurrentShamsiDate(): String {
        val cal = Calendar.getInstance()
        return gregorianToShamsi(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun getDayOfWeekNumber(dateStr: String): Int {
        val parts = dateStr.split("/").map { it.toInt() }
        val cal = shamsiToGregorian(parts[0], parts[1], parts[2])
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // تبدیل تقویم میلادی به ۱ برای شنبه تا ۷ برای جمعه
        return when (dow) {
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 2
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            Calendar.FRIDAY -> 7
            else -> 1
        }
    }

    fun getDayOfWeekName(dateStr: String): String {
        return getDayNameByNumber(getDayOfWeekNumber(dateStr))
    }

    fun getDayNameByNumber(dayNum: Int): String {
        return when (dayNum) {
            1 -> "شنبه"
            2 -> "یکشنبه"
            3 -> "دوشنبه"
            4 -> "سه‌شنبه"
            5 -> "چهارشنبه"
            6 -> "پنج‌شنبه"
            7 -> "جمعه"
            else -> ""
        }
    }

    fun getDayShortName(dayNum: Int): String {
        return when (dayNum) {
            1 -> "ش"
            2 -> "ی"
            3 -> "د"
            4 -> "س"
            5 -> "چ"
            6 -> "پ"
            7 -> "ج"
            else -> ""
        }
    }

    fun getNextDate(dateStr: String): String {
        val parts = dateStr.split("/").map { it.toInt() }
        val cal = shamsiToGregorian(parts[0], parts[1], parts[2])
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return gregorianToShamsi(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun getPreviousDate(dateStr: String): String {
        val parts = dateStr.split("/").map { it.toInt() }
        val cal = shamsiToGregorian(parts[0], parts[1], parts[2])
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return gregorianToShamsi(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun gregorianToShamsi(gYear: Int, gMonth: Int, gDay: Int): String {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDaysInMonth[i + 1]
        if (gm > 1 && ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0))) gDayNo++
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i + 1]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i + 1]
        }
        val jd = jDayNo + 1
        return String.format("%04d/%02d/%02d", jy, jm, jd)
    }

    private fun shamsiToGregorian(jYear: Int, jMonth: Int, jDay: Int): Calendar {
        var jy = jYear - 979
        var jm = jMonth - 1
        var jd = jDay - 1

        var jDayNo = 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4
        for (i in 0 until jm) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) gDayNo++
            else leap = false
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gDayNo >= gDaysInMonth[gm + 1]) {
            gDayNo -= gDaysInMonth[gm + 1]
            gm++
        }
        val gd = gDayNo + 1

        val cal = Calendar.getInstance()
        cal.set(gy, gm, gd)
        return cal
    }
}
