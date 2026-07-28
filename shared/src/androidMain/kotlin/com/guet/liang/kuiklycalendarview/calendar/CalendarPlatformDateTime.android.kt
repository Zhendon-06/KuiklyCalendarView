package com.guet.liang.kuiklycalendarview.calendar

import java.util.Calendar

internal actual object CalendarPlatformDateTime {
    actual fun currentDate(): CalendarDate {
        val calendar = Calendar.getInstance()
        return CalendarDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    actual fun startOfDayTimestampMillis(date: CalendarDate): Long =
        Calendar.getInstance().run {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
}
