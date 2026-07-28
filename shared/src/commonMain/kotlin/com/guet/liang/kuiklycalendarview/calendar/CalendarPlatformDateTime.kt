package com.guet.liang.kuiklycalendarview.calendar

/** Platform-local clock boundary used only for today and selection timestamps. */
internal expect object CalendarPlatformDateTime {
    fun currentDate(): CalendarDate

    fun startOfDayTimestampMillis(date: CalendarDate): Long
}
