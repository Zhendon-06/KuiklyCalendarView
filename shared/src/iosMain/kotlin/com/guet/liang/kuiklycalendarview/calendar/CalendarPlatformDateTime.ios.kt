package com.guet.liang.kuiklycalendarview.calendar

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSCalendar
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.date
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970

internal actual object CalendarPlatformDateTime {
    actual fun currentDate(): CalendarDate {
        val components = dateFormatter("yyyy-MM-dd")
            .stringFromDate(NSDate.date())
            .split("-")
        return CalendarDate(
            year = components[0].toInt(),
            month = components[1].toInt(),
            day = components[2].toInt(),
        )
    }

    actual fun startOfDayTimestampMillis(date: CalendarDate): Long {
        val value = "${date.year.toString().padStart(4, '0')}-" +
            "${date.month.toString().padStart(2, '0')}-" +
            "${date.day.toString().padStart(2, '0')} 12:00:00.000"
        val noon = checkNotNull(
            dateFormatter("yyyy-MM-dd HH:mm:ss.SSS").dateFromString(value),
        )
        val startOfDay = NSCalendar.currentCalendar.startOfDayForDate(noon)
        return (startOfDay.timeIntervalSince1970 * 1_000.0).toLong()
    }

    private fun dateFormatter(pattern: String): NSDateFormatter = NSDateFormatter().apply {
        locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
        timeZone = NSTimeZone.localTimeZone
        dateFormat = pattern
        lenient = true
    }
}
