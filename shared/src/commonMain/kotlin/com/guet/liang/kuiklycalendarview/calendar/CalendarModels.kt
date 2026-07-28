package com.guet.liang.kuiklycalendarview.calendar

/**
 * A validated calendar date in the proleptic Gregorian calendar.
 *
 * The model is independent of time zones and platform date APIs. Years start at 1, months use
 * the human-readable range 1 through 12, and [day] is validated against its month and year.
 * Natural ordering is chronological.
 *
 * @property year the Gregorian year, starting at 1.
 * @property month the month number in the range 1 through 12.
 * @property day the day of month, starting at 1.
 * @throws IllegalArgumentException if any component does not form a valid Gregorian date.
 */
public data class CalendarDate(
    public val year: Int,
    public val month: Int,
    public val day: Int,
) : Comparable<CalendarDate> {

    init {
        require(year >= 1) { "year must be at least 1, but was $year" }
        require(month in 1..12) { "month must be in 1..12, but was $month" }
        val maximumDay = CalendarMath.daysInMonth(year, month)
        require(day in 1..maximumDay) {
            "day must be in 1..$maximumDay for $year-$month, but was $day"
        }
    }

    /** The [CalendarMonth] containing this date. */
    public val calendarMonth: CalendarMonth
        get() = CalendarMonth(year, month)

    /**
     * Compares this date with [other] in chronological order.
     *
     * @return a negative value when this date is earlier, zero when equal, or a positive value
     * when this date is later.
     */
    override fun compareTo(other: CalendarDate): Int {
        val yearComparison = year.compareTo(other.year)
        if (yearComparison != 0) return yearComparison

        val monthComparison = month.compareTo(other.month)
        if (monthComparison != 0) return monthComparison

        return day.compareTo(other.day)
    }

    /** Factory-independent helpers for validating date components. */
    public companion object {
        /**
         * Reports whether the supplied components form a supported Gregorian date.
         *
         * Unlike the constructor, this helper never throws for invalid components.
         *
         * @param year the candidate Gregorian year.
         * @param month the candidate month number.
         * @param day the candidate day of month.
         * @return `true` only when constructing [CalendarDate] with these values would succeed.
         */
        public fun isValid(year: Int, month: Int, day: Int): Boolean {
            if (year < 1 || month !in 1..12) return false
            return day in 1..CalendarMath.daysInMonth(year, month)
        }
    }
}

/**
 * A validated month in the proleptic Gregorian calendar.
 *
 * Natural ordering is chronological and [plusMonths] can move in either direction across year
 * boundaries.
 *
 * @property year the Gregorian year, starting at 1.
 * @property month the month number in the range 1 through 12.
 * @throws IllegalArgumentException if [year] or [month] is outside its supported range.
 */
public data class CalendarMonth(
    public val year: Int,
    public val month: Int,
) : Comparable<CalendarMonth> {

    init {
        require(year >= 1) { "year must be at least 1, but was $year" }
        require(month in 1..12) { "month must be in 1..12, but was $month" }
    }

    /** The number of valid dates in this month. */
    public val numberOfDays: Int
        get() = CalendarMath.daysInMonth(year, month)

    /** The first date in this month. */
    public val firstDate: CalendarDate
        get() = CalendarDate(year, month, 1)

    /**
     * Creates a validated date in this month.
     *
     * @param day the requested day of month.
     * @return the date represented by this month and [day].
     * @throws IllegalArgumentException if [day] is not valid in this month.
     */
    public fun atDay(day: Int): CalendarDate = CalendarDate(year, month, day)

    /**
     * Returns a month shifted by [months] calendar months.
     *
     * Positive values move forward and negative values move backward. Day-of-month adjustment is
     * deliberately not part of this month-only operation.
     *
     * @param months the signed number of months to add.
     * @return the shifted calendar month.
     * @throws IllegalArgumentException if the result would be before year 1 or after [Int.MAX_VALUE].
     */
    public fun plusMonths(months: Int): CalendarMonth {
        val zeroBasedMonth = (year.toLong() - 1L) * MONTHS_PER_YEAR + (month - 1L)
        val shiftedMonth = zeroBasedMonth + months.toLong()
        require(shiftedMonth >= 0L) { "resulting month must not be before year 1" }

        val shiftedYear = shiftedMonth / MONTHS_PER_YEAR + 1L
        require(shiftedYear <= Int.MAX_VALUE.toLong()) {
            "resulting year must not exceed ${Int.MAX_VALUE}"
        }
        val monthOfYear = (shiftedMonth % MONTHS_PER_YEAR + 1L).toInt()
        return CalendarMonth(shiftedYear.toInt(), monthOfYear)
    }

    /**
     * Compares this month with [other] in chronological order.
     *
     * @return a negative value when this month is earlier, zero when equal, or a positive value
     * when this month is later.
     */
    override fun compareTo(other: CalendarMonth): Int {
        val yearComparison = year.compareTo(other.year)
        return if (yearComparison != 0) yearComparison else month.compareTo(other.month)
    }

    private companion object {
        private const val MONTHS_PER_YEAR = 12L
    }
}

/**
 * A day of the Gregorian seven-day week.
 *
 * @property isoValue the ISO-8601 weekday number, where Monday is 1 and Sunday is 7.
 */
public enum class CalendarDayOfWeek(public val isoValue: Int) {
    /** Monday, ISO weekday 1. */
    MONDAY(1),

    /** Tuesday, ISO weekday 2. */
    TUESDAY(2),

    /** Wednesday, ISO weekday 3. */
    WEDNESDAY(3),

    /** Thursday, ISO weekday 4. */
    THURSDAY(4),

    /** Friday, ISO weekday 5. */
    FRIDAY(5),

    /** Saturday, ISO weekday 6. */
    SATURDAY(6),

    /** Sunday, ISO weekday 7. */
    SUNDAY(7),
}

/** The first weekday displayed by a calendar grid. */
public enum class CalendarWeekStart {
    /** Displays Sunday in the first column. */
    SUNDAY,

    /** Displays Monday in the first column. */
    MONDAY;

    /** The weekday represented by the first grid column. */
    public val firstDay: CalendarDayOfWeek
        get() = when (this) {
            SUNDAY -> CalendarDayOfWeek.SUNDAY
            MONDAY -> CalendarDayOfWeek.MONDAY
        }

    /**
     * Returns all weekdays in their display order for this week configuration.
     *
     * @return seven unique weekdays starting with [firstDay].
     */
    public fun orderedDays(): List<CalendarDayOfWeek> = when (this) {
        SUNDAY -> listOf(
            CalendarDayOfWeek.SUNDAY,
            CalendarDayOfWeek.MONDAY,
            CalendarDayOfWeek.TUESDAY,
            CalendarDayOfWeek.WEDNESDAY,
            CalendarDayOfWeek.THURSDAY,
            CalendarDayOfWeek.FRIDAY,
            CalendarDayOfWeek.SATURDAY,
        )

        MONDAY -> listOf(
            CalendarDayOfWeek.MONDAY,
            CalendarDayOfWeek.TUESDAY,
            CalendarDayOfWeek.WEDNESDAY,
            CalendarDayOfWeek.THURSDAY,
            CalendarDayOfWeek.FRIDAY,
            CalendarDayOfWeek.SATURDAY,
            CalendarDayOfWeek.SUNDAY,
        )
    }
}

/** Describes which month a grid cell belongs to relative to the displayed month. */
public enum class CalendarDayRelation {
    /** The date belongs to the month immediately before the displayed month. */
    PREVIOUS_MONTH,

    /** The date belongs to the displayed month. */
    CURRENT_MONTH,

    /** The date belongs to the month immediately after the displayed month. */
    NEXT_MONTH,
}

/**
 * One date cell in a [CalendarMonthGrid].
 *
 * @property date the exact Gregorian date represented by this cell.
 * @property relation the date's relation to the grid's displayed month.
 * @property dayOfWeek the weekday of [date].
 * @property row the zero-based row index in the range 0 through 5.
 * @property column the zero-based column index in the range 0 through 6.
 * @throws IllegalArgumentException if [row] or [column] is outside the fixed grid bounds.
 */
public data class CalendarDayCell(
    public val date: CalendarDate,
    public val relation: CalendarDayRelation,
    public val dayOfWeek: CalendarDayOfWeek,
    public val row: Int,
    public val column: Int,
) {

    init {
        require(row in 0 until CalendarMonthGrid.WEEK_COUNT) {
            "row must be in 0 until ${CalendarMonthGrid.WEEK_COUNT}, but was $row"
        }
        require(column in 0 until CalendarMonthGrid.DAYS_PER_WEEK) {
            "column must be in 0 until ${CalendarMonthGrid.DAYS_PER_WEEK}, but was $column"
        }
        require(dayOfWeek == CalendarMath.dayOfWeek(date)) {
            "dayOfWeek must match date $date"
        }
    }

    /** Whether this date belongs to the grid's displayed month. */
    public val isCurrentMonth: Boolean
        get() = relation == CalendarDayRelation.CURRENT_MONTH
}

/**
 * A complete, fixed-size month grid containing six rows and seven columns.
 *
 * Cells before and after [month] are real dates from adjacent months. [cells] is always ordered
 * from left to right and top to bottom.
 *
 * @property month the month represented by the grid.
 * @property weekStart the weekday used by the first column.
 * @property cells all 42 date cells in display order.
 * @throws IllegalArgumentException if [cells] does not describe a correctly ordered 6-by-7 grid.
 */
public data class CalendarMonthGrid(
    public val month: CalendarMonth,
    public val weekStart: CalendarWeekStart,
    public val cells: List<CalendarDayCell>,
) {

    init {
        require(cells.size == CELL_COUNT) {
            "a month grid must contain exactly $CELL_COUNT cells, but contained ${cells.size}"
        }
        val orderedDays = weekStart.orderedDays()
        cells.forEachIndexed { index, cell ->
            val expectedRow = index / DAYS_PER_WEEK
            val expectedColumn = index % DAYS_PER_WEEK
            require(cell.row == expectedRow && cell.column == expectedColumn) {
                "cell $index must be at row $expectedRow, column $expectedColumn"
            }
            require(cell.dayOfWeek == orderedDays[expectedColumn]) {
                "cell $index weekday must match column $expectedColumn"
            }
            require(cell.relation == relationOf(cell.date, month)) {
                "cell $index relation does not match its date and displayed month"
            }
            if (index > 0) {
                require(areConsecutive(cells[index - 1].date, cell.date)) {
                    "cell $index must immediately follow cell ${index - 1}"
                }
            }
        }
        require(cells.count { it.isCurrentMonth } == month.numberOfDays) {
            "grid must contain every date in the displayed month exactly once"
        }
    }

    /** Alias for [cells] for callers that use day-oriented terminology. */
    public val days: List<CalendarDayCell>
        get() = cells

    /** The seven weekdays in column display order. */
    public val weekdays: List<CalendarDayOfWeek>
        get() = weekStart.orderedDays()

    /** The cells grouped into six lists of seven dates. */
    public val weeks: List<List<CalendarDayCell>>
        get() = cells.chunked(DAYS_PER_WEEK)

    /**
     * Returns one zero-based week row.
     *
     * @param row the row index in the range 0 through 5.
     * @return seven cells in left-to-right display order.
     * @throws IndexOutOfBoundsException if [row] is outside the six grid rows.
     */
    public fun week(row: Int): List<CalendarDayCell> {
        if (row !in 0 until WEEK_COUNT) {
            throw IndexOutOfBoundsException("week row $row is outside the six-row grid")
        }
        val startIndex = row * DAYS_PER_WEEK
        return cells.subList(startIndex, startIndex + DAYS_PER_WEEK)
    }

    /**
     * Returns the cell at a zero-based [row] and [column].
     *
     * @throws IndexOutOfBoundsException if either coordinate is outside the 6-by-7 grid.
     */
    public operator fun get(row: Int, column: Int): CalendarDayCell {
        if (row !in 0 until WEEK_COUNT || column !in 0 until DAYS_PER_WEEK) {
            throw IndexOutOfBoundsException("grid position ($row, $column) is outside the 6-by-7 grid")
        }
        return cells[row * DAYS_PER_WEEK + column]
    }

    /** Fixed dimensions shared by every month grid. */
    public companion object {
        /** The number of columns in a calendar week. */
        public const val DAYS_PER_WEEK: Int = 7

        /** The number of rows displayed for every month. */
        public const val WEEK_COUNT: Int = 6

        /** The total number of cells in every month grid. */
        public const val CELL_COUNT: Int = DAYS_PER_WEEK * WEEK_COUNT

        private fun relationOf(
            date: CalendarDate,
            displayedMonth: CalendarMonth,
        ): CalendarDayRelation = when {
            date.calendarMonth < displayedMonth -> CalendarDayRelation.PREVIOUS_MONTH
            date.calendarMonth > displayedMonth -> CalendarDayRelation.NEXT_MONTH
            else -> CalendarDayRelation.CURRENT_MONTH
        }

        private fun areConsecutive(first: CalendarDate, second: CalendarDate): Boolean {
            if (first.calendarMonth == second.calendarMonth) {
                return second.day == first.day + 1
            }
            if (first.day != first.calendarMonth.numberOfDays || second.day != 1) return false
            return first.calendarMonth.plusMonths(1) == second.calendarMonth
        }
    }
}

/**
 * Platform-independent Gregorian calendar calculations used by the calendar component.
 *
 * Every operation is deterministic and does not read a clock, locale, or time zone.
 */
public object CalendarMath {

    /**
     * Reports whether [year] is a Gregorian leap year.
     *
     * A year is a leap year when divisible by 4, except century years not divisible by 400.
     *
     * @param year the Gregorian year, starting at 1.
     * @return `true` when February contains 29 days.
     * @throws IllegalArgumentException if [year] is less than 1.
     */
    public fun isLeapYear(year: Int): Boolean {
        require(year >= 1) { "year must be at least 1, but was $year" }
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    /**
     * Returns the number of days in a Gregorian month.
     *
     * @param year the Gregorian year, starting at 1.
     * @param month the month number in the range 1 through 12.
     * @return a value from 28 through 31.
     * @throws IllegalArgumentException if [year] or [month] is outside its supported range.
     */
    public fun daysInMonth(year: Int, month: Int): Int {
        require(year >= 1) { "year must be at least 1, but was $year" }
        require(month in 1..12) { "month must be in 1..12, but was $month" }
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    /**
     * Calculates the weekday of [date] using Gregorian calendar rules.
     *
     * @param date a validated Gregorian date.
     * @return the weekday containing [date].
     */
    public fun dayOfWeek(date: CalendarDate): CalendarDayOfWeek {
        var adjustedYear = date.year.toLong()
        if (date.month < 3) adjustedYear -= 1L
        val sundayBasedIndex = (
            adjustedYear +
                adjustedYear / 4L -
                adjustedYear / 100L +
                adjustedYear / 400L +
                MONTH_OFFSETS[date.month - 1] +
                date.day
            ).mod(DAYS_PER_WEEK)
        return when (sundayBasedIndex) {
            0 -> CalendarDayOfWeek.SUNDAY
            1 -> CalendarDayOfWeek.MONDAY
            2 -> CalendarDayOfWeek.TUESDAY
            3 -> CalendarDayOfWeek.WEDNESDAY
            4 -> CalendarDayOfWeek.THURSDAY
            5 -> CalendarDayOfWeek.FRIDAY
            else -> CalendarDayOfWeek.SATURDAY
        }
    }

    /**
     * Validates a date and calculates its Gregorian weekday.
     *
     * @param year the Gregorian year, starting at 1.
     * @param month the month number in the range 1 through 12.
     * @param day the day of month, starting at 1.
     * @return the weekday containing the requested date.
     * @throws IllegalArgumentException if the components do not form a valid date.
     */
    public fun dayOfWeek(year: Int, month: Int, day: Int): CalendarDayOfWeek =
        dayOfWeek(CalendarDate(year, month, day))

    /**
     * Builds a fixed 42-cell grid for [month].
     *
     * Leading and trailing cells contain actual dates from adjacent months. The first cell always
     * matches [weekStart], and every following row covers the next seven consecutive dates.
     *
     * @param month the month to display.
     * @param weekStart whether Sunday or Monday occupies the first column.
     * @return a complete six-week month grid.
     * @throws IllegalArgumentException if an adjacent month would fall outside supported years.
     */
    public fun buildMonthGrid(
        month: CalendarMonth,
        weekStart: CalendarWeekStart = CalendarWeekStart.MONDAY,
    ): CalendarMonthGrid {
        val firstWeekday = dayOfWeek(month.firstDate)
        val leadingCellCount = (
            firstWeekday.isoValue - weekStart.firstDay.isoValue + DAYS_PER_WEEK
            ) % DAYS_PER_WEEK
        val previousMonth = if (leadingCellCount == 0) null else month.plusMonths(-1)
        val nextMonth = month.plusMonths(1)
        val orderedDays = weekStart.orderedDays()
        val cells = List(CalendarMonthGrid.CELL_COUNT) { index ->
            val relativeDay = index - leadingCellCount + 1
            val date: CalendarDate
            val relation: CalendarDayRelation
            when {
                relativeDay <= 0 -> {
                    val adjacentMonth = checkNotNull(previousMonth)
                    date = adjacentMonth.atDay(adjacentMonth.numberOfDays + relativeDay)
                    relation = CalendarDayRelation.PREVIOUS_MONTH
                }

                relativeDay > month.numberOfDays -> {
                    date = nextMonth.atDay(relativeDay - month.numberOfDays)
                    relation = CalendarDayRelation.NEXT_MONTH
                }

                else -> {
                    date = month.atDay(relativeDay)
                    relation = CalendarDayRelation.CURRENT_MONTH
                }
            }
            CalendarDayCell(
                date = date,
                relation = relation,
                dayOfWeek = orderedDays[index % DAYS_PER_WEEK],
                row = index / DAYS_PER_WEEK,
                column = index % DAYS_PER_WEEK,
            )
        }
        return CalendarMonthGrid(month, weekStart, cells)
    }

    /**
     * Validates a year and month and builds its fixed 42-cell grid.
     *
     * @param year the Gregorian year, starting at 1.
     * @param month the month number in the range 1 through 12.
     * @param weekStart whether Sunday or Monday occupies the first column.
     * @return a complete six-week month grid.
     * @throws IllegalArgumentException if the requested or adjacent month is unsupported.
     */
    public fun buildMonthGrid(
        year: Int,
        month: Int,
        weekStart: CalendarWeekStart = CalendarWeekStart.MONDAY,
    ): CalendarMonthGrid = buildMonthGrid(CalendarMonth(year, month), weekStart)

    private const val DAYS_PER_WEEK = 7
    private val MONTH_OFFSETS = listOf(0L, 3L, 2L, 5L, 0L, 3L, 5L, 1L, 4L, 6L, 2L, 4L)
}
