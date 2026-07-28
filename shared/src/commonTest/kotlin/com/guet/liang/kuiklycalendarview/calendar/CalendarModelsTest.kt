package com.guet.liang.kuiklycalendarview.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarModelsTest {

    @Test
    fun dateValidationAcceptsOnlyRealGregorianDates() {
        assertTrue(CalendarDate.isValid(2024, 2, 29))
        assertTrue(CalendarDate.isValid(2000, 2, 29))
        assertFalse(CalendarDate.isValid(2023, 2, 29))
        assertFalse(CalendarDate.isValid(1900, 2, 29))
        assertFalse(CalendarDate.isValid(2024, 4, 31))
        assertFalse(CalendarDate.isValid(2024, 13, 1))
        assertFalse(CalendarDate.isValid(2024, 1, 0))
        assertFalse(CalendarDate.isValid(0, 1, 1))

        assertFailsWith<IllegalArgumentException> { CalendarDate(2023, 2, 29) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2024, 0, 1) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2024, 1, 32) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(0, 1, 1) }
    }

    @Test
    fun leapYearAndMonthLengthsUseGregorianRules() {
        assertTrue(CalendarMath.isLeapYear(2024))
        assertTrue(CalendarMath.isLeapYear(2000))
        assertFalse(CalendarMath.isLeapYear(2023))
        assertFalse(CalendarMath.isLeapYear(1900))
        assertEquals(29, CalendarMath.daysInMonth(2024, 2))
        assertEquals(28, CalendarMath.daysInMonth(2023, 2))
        assertEquals(30, CalendarMath.daysInMonth(2024, 4))
        assertEquals(31, CalendarMath.daysInMonth(2024, 12))

        assertFailsWith<IllegalArgumentException> { CalendarMath.isLeapYear(0) }
        assertFailsWith<IllegalArgumentException> { CalendarMath.daysInMonth(2024, 13) }
    }

    @Test
    fun datesAndMonthsUseChronologicalOrdering() {
        assertTrue(CalendarDate(2023, 12, 31) < CalendarDate(2024, 1, 1))
        assertTrue(CalendarDate(2024, 2, 28) < CalendarDate(2024, 2, 29))
        assertEquals(0, CalendarDate(2024, 7, 28).compareTo(CalendarDate(2024, 7, 28)))
        assertTrue(CalendarMonth(2023, 12) < CalendarMonth(2024, 1))
        assertTrue(CalendarMonth(2024, 6) < CalendarMonth(2024, 7))
        assertEquals(CalendarMonth(2024, 7), CalendarDate(2024, 7, 28).calendarMonth)
    }

    @Test
    fun plusMonthsMovesAcrossYearBoundariesInBothDirections() {
        assertEquals(CalendarMonth(2025, 1), CalendarMonth(2024, 12).plusMonths(1))
        assertEquals(CalendarMonth(2023, 12), CalendarMonth(2024, 1).plusMonths(-1))
        assertEquals(CalendarMonth(2026, 3), CalendarMonth(2024, 1).plusMonths(26))
        assertEquals(CalendarMonth(2021, 11), CalendarMonth(2024, 1).plusMonths(-26))
        assertEquals(CalendarMonth(2024, 7), CalendarMonth(2024, 7).plusMonths(0))

        assertFailsWith<IllegalArgumentException> { CalendarMonth(1, 1).plusMonths(-1) }
        assertFailsWith<IllegalArgumentException> {
            CalendarMonth(Int.MAX_VALUE, 12).plusMonths(1)
        }
    }

    @Test
    fun dayOfWeekMatchesKnownGregorianDates() {
        assertEquals(CalendarDayOfWeek.MONDAY, CalendarMath.dayOfWeek(1, 1, 1))
        assertEquals(CalendarDayOfWeek.THURSDAY, CalendarMath.dayOfWeek(1970, 1, 1))
        assertEquals(CalendarDayOfWeek.SATURDAY, CalendarMath.dayOfWeek(2000, 1, 1))
        assertEquals(CalendarDayOfWeek.THURSDAY, CalendarMath.dayOfWeek(2024, 2, 29))
        assertEquals(CalendarDayOfWeek.SUNDAY, CalendarMath.dayOfWeek(2024, 9, 1))
    }

    @Test
    fun weekStartsExposeTheExpectedHeaderOrder() {
        assertEquals(
            listOf(
                CalendarDayOfWeek.SUNDAY,
                CalendarDayOfWeek.MONDAY,
                CalendarDayOfWeek.TUESDAY,
                CalendarDayOfWeek.WEDNESDAY,
                CalendarDayOfWeek.THURSDAY,
                CalendarDayOfWeek.FRIDAY,
                CalendarDayOfWeek.SATURDAY,
            ),
            CalendarWeekStart.SUNDAY.orderedDays(),
        )
        assertEquals(CalendarDayOfWeek.MONDAY, CalendarWeekStart.MONDAY.orderedDays().first())
        assertEquals(CalendarDayOfWeek.SUNDAY, CalendarWeekStart.MONDAY.orderedDays().last())
    }

    @Test
    fun sundayFirstGridStartsOnSundayAndAlwaysContainsFortyTwoCells() {
        val grid = CalendarMath.buildMonthGrid(2024, 9, CalendarWeekStart.SUNDAY)

        assertEquals(CalendarMonth(2024, 9), grid.month)
        assertEquals(42, grid.cells.size)
        assertEquals(6, grid.weeks.size)
        assertTrue(grid.weeks.all { it.size == 7 })
        assertEquals(CalendarDate(2024, 9, 1), grid.cells.first().date)
        assertEquals(CalendarDate(2024, 10, 12), grid.cells.last().date)
        assertEquals(CalendarDayRelation.CURRENT_MONTH, grid.cells.first().relation)
        assertEquals(CalendarDayRelation.NEXT_MONTH, grid.cells.last().relation)
        assertEquals(30, grid.cells.count { it.isCurrentMonth })
        assertEquals(grid.cells, grid.days)
    }

    @Test
    fun mondayFirstGridIncludesLeadingDatesFromPreviousMonth() {
        val grid = CalendarMath.buildMonthGrid(2024, 9, CalendarWeekStart.MONDAY)

        assertEquals(CalendarDate(2024, 8, 26), grid[0, 0].date)
        assertEquals(CalendarDate(2024, 9, 1), grid[0, 6].date)
        assertEquals(CalendarDate(2024, 10, 6), grid[5, 6].date)
        assertEquals(CalendarDayRelation.PREVIOUS_MONTH, grid[0, 0].relation)
        assertEquals(CalendarDayRelation.CURRENT_MONTH, grid[0, 6].relation)
        assertEquals(CalendarDayRelation.NEXT_MONTH, grid[5, 6].relation)
        assertEquals(grid.cells.take(7), grid.week(0))
        assertEquals(CalendarDayOfWeek.MONDAY, grid.weekdays.first())
        assertEquals(CalendarDayOfWeek.SUNDAY, grid.weekdays.last())
    }

    @Test
    fun leapFebruaryGridContainsAllTwentyNineDates() {
        val grid = CalendarMath.buildMonthGrid(CalendarMonth(2024, 2))

        assertEquals(CalendarDate(2024, 1, 29), grid.cells.first().date)
        assertEquals(CalendarDate(2024, 3, 10), grid.cells.last().date)
        assertEquals(3, grid.cells.count { it.relation == CalendarDayRelation.PREVIOUS_MONTH })
        assertEquals(29, grid.cells.count { it.relation == CalendarDayRelation.CURRENT_MONTH })
        assertEquals(10, grid.cells.count { it.relation == CalendarDayRelation.NEXT_MONTH })
        assertEquals(CalendarDate(2024, 2, 29), grid.cells.first { it.date.day == 29 && it.isCurrentMonth }.date)
    }

    @Test
    fun gridCrossesDecemberAndJanuaryWithoutLosingDateOrder() {
        val december = CalendarMath.buildMonthGrid(2023, 12, CalendarWeekStart.MONDAY)
        assertEquals(CalendarDate(2023, 11, 27), december.cells.first().date)
        assertEquals(CalendarDate(2024, 1, 7), december.cells.last().date)

        val january = CalendarMath.buildMonthGrid(2024, 1, CalendarWeekStart.SUNDAY)
        assertEquals(CalendarDate(2023, 12, 31), january.cells.first().date)
        assertEquals(CalendarDate(2024, 2, 10), january.cells.last().date)
        january.cells.forEachIndexed { index, cell ->
            assertEquals(index / 7, cell.row)
            assertEquals(index % 7, cell.column)
            assertEquals(CalendarMath.dayOfWeek(cell.date), cell.dayOfWeek)
        }
    }

    @Test
    fun gridCoordinatesAndCellWeekdaysAreValidated() {
        assertFailsWith<IllegalArgumentException> {
            CalendarDayCell(
                date = CalendarDate(2024, 7, 28),
                relation = CalendarDayRelation.CURRENT_MONTH,
                dayOfWeek = CalendarDayOfWeek.MONDAY,
                row = 0,
                column = 0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            CalendarMonthGrid(
                month = CalendarMonth(2024, 7),
                weekStart = CalendarWeekStart.MONDAY,
                cells = emptyList(),
            )
        }
        val grid = CalendarMath.buildMonthGrid(2024, 7)
        assertFailsWith<IndexOutOfBoundsException> { grid.week(6) }
        assertFailsWith<IndexOutOfBoundsException> { grid[-1, 0] }
    }
}
