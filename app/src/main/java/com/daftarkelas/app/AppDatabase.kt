    // دریافت آخرین یادداشت ثبت‌شده برای جلسه قبلیِ همین درس (بر اساس تاریخ‌های قبل از امروز)
    @Query("""
        SELECT noteForNextSession FROM lesson_plans 
        WHERE scheduleId = :scheduleId AND dateStr < :currentDate AND noteForNextSession != '' 
        ORDER BY dateStr DESC LIMIT 1
    """)
    fun getPreviousNote(scheduleId: Long, currentDate: String): String?
