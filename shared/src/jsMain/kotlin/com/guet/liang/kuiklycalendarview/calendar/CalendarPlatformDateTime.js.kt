package com.guet.liang.kuiklycalendarview.calendar

import kotlin.js.Date

internal actual object CalendarPlatformDateTime {
    actual fun currentDate(): CalendarDate {
        val date = Date()
        return CalendarDate(
            year = date.getFullYear(),
            month = date.getMonth() + 1,
            day = date.getDate(),
        )
    }

    actual fun startOfDayTimestampMillis(date: CalendarDate): Long {
        val localDate = Date(
            year = date.year,
            month = date.month - 1,
            day = date.day,
            hour = 0,
            minute = 0,
            second = 0,
            millisecond = 0,
        )
        return localDate.getTime().toLong()
    }
}
