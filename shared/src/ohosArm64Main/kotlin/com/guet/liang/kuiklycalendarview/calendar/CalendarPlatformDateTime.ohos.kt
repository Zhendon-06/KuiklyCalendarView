package com.guet.liang.kuiklycalendarview.calendar

import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar

internal actual object CalendarPlatformDateTime {
    actual fun currentDate(): CalendarDate {
        val calendar = newCalendar()
        return CalendarDate(
            year = calendar.get(ICalendar.Field.YEAR),
            month = calendar.get(ICalendar.Field.MONTH) + 1,
            day = calendar.get(ICalendar.Field.DAY_OF_MONTH),
        )
    }

    actual fun startOfDayTimestampMillis(date: CalendarDate): Long {
        val calendar = newCalendar()
        calendar.set(ICalendar.Field.DAY_OF_MONTH, 1)
        calendar.set(ICalendar.Field.YEAR, date.year)
        calendar.set(ICalendar.Field.MONTH, date.month - 1)
        calendar.set(ICalendar.Field.DAY_OF_MONTH, date.day)
        calendar.set(ICalendar.Field.HOUR_OF_DAY, 0)
        calendar.set(ICalendar.Field.MINUS, 0)
        calendar.set(ICalendar.Field.SECOND, 0)
        calendar.set(ICalendar.Field.MILLISECOND, 0)
        return calendar.timeInMillis()
    }

    private fun newCalendar(): ICalendar = PagerManager
        .getCurrentPager()
        .acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
        .newCalendarInstance()
}
