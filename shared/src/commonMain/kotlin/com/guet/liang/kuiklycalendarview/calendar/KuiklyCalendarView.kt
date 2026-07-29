package com.guet.liang.kuiklycalendarview.calendar

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderRectRadius
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Describes how a visible month change was initiated. */
enum class CalendarNavigationSource {
    PREVIOUS_BUTTON,
    NEXT_BUTTON,
    DATE_CELL,
    ADJACENT_DATE,
    TODAY_BUTTON,
    PROGRAMMATIC,
}

/** Result delivered after a date is selected. */
data class CalendarSelectionResult(
    val date: CalendarDate,
    val timestampMillis: Long,
    val displayedMonth: CalendarMonth,
    val isToday: Boolean,
    val source: CalendarNavigationSource,
) {
    /** ISO-8601 calendar date, for example `2026-07-28`. */
    val isoDate: String
        get() = date.toIsoDate()

    /** Gregorian weekday of [date]. */
    val dayOfWeek: CalendarDayOfWeek
        get() = CalendarMath.dayOfWeek(date)
}

/** Result delivered after the component changes its visible month. */
data class CalendarMonthChangeResult(
    val previousMonth: CalendarMonth,
    val month: CalendarMonth,
    val source: CalendarNavigationSource,
)

/**
 * Localized strings used by [KuiklyCalendarView]. Weekday labels must be ordered
 * from Sunday through Saturday; [CalendarWeekStart] rotates them when rendered.
 */
class CalendarLocale(
    val weekdaysSundayFirst: List<String>,
    val todayText: String,
    val selectedPrefix: String,
    val emptySelectionText: String,
    val headerHint: String,
    private val monthTitleFormatter: (CalendarMonth) -> String,
) {
    init {
        require(weekdaysSundayFirst.size == 7) { "Exactly seven weekday labels are required" }
    }

    fun monthTitle(month: CalendarMonth): String = monthTitleFormatter(month)

    fun weekdayTitles(weekStart: CalendarWeekStart): List<String> = when (weekStart) {
        CalendarWeekStart.SUNDAY -> weekdaysSundayFirst
        CalendarWeekStart.MONDAY -> weekdaysSundayFirst.drop(1) + weekdaysSundayFirst.first()
    }

    companion object {
        /** Simplified Chinese locale. */
        val ZH_CN = CalendarLocale(
            weekdaysSundayFirst = listOf("日", "一", "二", "三", "四", "五", "六"),
            todayText = "回到今天",
            selectedPrefix = "已选择",
            emptySelectionText = "请选择日期",
            headerHint = "规划你的每一天",
            monthTitleFormatter = { "${it.year} 年 ${it.month} 月" },
        )

        /** English locale. */
        val EN_US = CalendarLocale(
            weekdaysSundayFirst = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            todayText = "Today",
            selectedPrefix = "Selected",
            emptySelectionText = "Choose a date",
            headerHint = "Plan your days",
            monthTitleFormatter = {
                "${ENGLISH_MONTHS[it.month - 1]} ${it.year}"
            },
        )

        private val ENGLISH_MONTHS = listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        )
    }
}

/** Visual tokens for [KuiklyCalendarView]. */
class CalendarStyle(
    val surfaceColor: Color = Color(0xFFFFFFFFL),
    val headerStartColor: Color = Color(0xFF635BFFL),
    val headerEndColor: Color = Color(0xFF8B5CF6L),
    val primaryColor: Color = Color(0xFF635BFFL),
    val primarySoftColor: Color = Color(0xFFF0EFFFFL),
    val headerTextColor: Color = Color.WHITE,
    val headerSecondaryTextColor: Color = Color(0xD9FFFFFFL),
    val dateTextColor: Color = Color(0xFF1E293BL),
    val secondaryTextColor: Color = Color(0xFF64748BL),
    val adjacentDateTextColor: Color = Color(0xFFCBD5E1L),
    val disabledDateTextColor: Color = Color(0xFFE2E8F0L),
    val weekendTextColor: Color = Color(0xFFF97316L),
    val dividerColor: Color = Color(0xFFEFF2F7L),
    val indicatorColor: Color = Color(0xFFF59E0BL),
    val cornerRadius: Float = 24f,
    val headerHeight: Float = 112f,
    val weekdayHeight: Float = 38f,
    val dayCellHeight: Float = 44f,
    val dayBubbleSize: Float = 36f,
    val horizontalPadding: Float = 16f,
)

/** Attributes supported by [KuiklyCalendarView]. */
class CalendarAttr : ComposeAttr() {
    internal var initialMonth: CalendarMonth? by observable(null)
    internal var initialSelectedDate: CalendarDate? by observable(null)
    internal var minDate: CalendarDate? by observable(null)
    internal var maxDate: CalendarDate? by observable(null)
    internal var weekStart: CalendarWeekStart by observable(CalendarWeekStart.SUNDAY)
    internal var showAdjacentMonthDates: Boolean by observable(true)
    internal var allowAdjacentMonthSelection: Boolean by observable(true)
    internal var showFooter: Boolean by observable(true)
    internal var selectTodayByDefault: Boolean by observable(true)
    internal var locale: CalendarLocale by observable(CalendarLocale.ZH_CN)
    internal var calendarStyle: CalendarStyle by observable(CalendarStyle())
    internal var dateEnabledPredicate: (CalendarDate) -> Boolean by observable({ true })
    internal var dateIndicatorPredicate: (CalendarDate) -> Boolean by observable({ false })

    /** Sets the initially visible month. The current local month is used by default. */
    fun initialMonth(month: CalendarMonth) {
        require(month.year in CALENDAR_VIEW_YEAR_RANGE) {
            "Calendar view supports years ${CALENDAR_VIEW_YEAR_RANGE.first}..${CALENDAR_VIEW_YEAR_RANGE.last}"
        }
        initialMonth = month
    }

    /** Sets the initial selection and uses its month when no initial month is supplied. */
    fun initialSelectedDate(date: CalendarDate?) {
        require(date == null || date.year in CALENDAR_VIEW_YEAR_RANGE) {
            "Calendar view supports years ${CALENDAR_VIEW_YEAR_RANGE.first}..${CALENDAR_VIEW_YEAR_RANGE.last}"
        }
        initialSelectedDate = date
    }

    /** Restricts selectable dates and month navigation to the inclusive range. */
    fun dateRange(minDate: CalendarDate? = null, maxDate: CalendarDate? = null) {
        require(minDate == null || maxDate == null || minDate <= maxDate) {
            "minDate must not be after maxDate"
        }
        require(minDate == null || minDate.year in CALENDAR_VIEW_YEAR_RANGE) {
            "minDate is outside the supported calendar view year range"
        }
        require(maxDate == null || maxDate.year in CALENDAR_VIEW_YEAR_RANGE) {
            "maxDate is outside the supported calendar view year range"
        }
        this.minDate = minDate
        this.maxDate = maxDate
    }

    /** Chooses whether the weekday header starts on Sunday or Monday. */
    fun weekStart(weekStart: CalendarWeekStart) {
        this.weekStart = weekStart
    }

    /** Shows or hides dates belonging to the previous and next months. */
    fun showAdjacentMonthDates(show: Boolean) {
        showAdjacentMonthDates = show
    }

    /** Enables selecting a visible adjacent-month date and navigating to that month. */
    fun allowAdjacentMonthSelection(allow: Boolean) {
        allowAdjacentMonthSelection = allow
    }

    /** Shows the selected-date summary and Today shortcut below the grid. */
    fun showFooter(show: Boolean) {
        showFooter = show
    }

    /** Controls whether today is selected automatically when no initial selection exists. */
    fun selectTodayByDefault(select: Boolean) {
        selectTodayByDefault = select
    }

    /** Applies localized labels and month-title formatting. */
    fun locale(locale: CalendarLocale) {
        this.locale = locale
    }

    /** Applies a complete visual token set. */
    fun style(style: CalendarStyle) {
        calendarStyle = style
    }

    /** Supplies an additional business rule for disabling individual dates. */
    fun dateEnabled(predicate: (CalendarDate) -> Boolean) {
        dateEnabledPredicate = predicate
    }

    /** Marks matching dates with a small event indicator. */
    fun dateIndicator(predicate: (CalendarDate) -> Boolean) {
        dateIndicatorPredicate = predicate
    }
}

/** Typed events emitted by [KuiklyCalendarView]. */
class CalendarEvent : ComposeEvent() {
    internal var dateSelectedHandler: ((CalendarSelectionResult) -> Unit)? = null
    internal var monthChangedHandler: ((CalendarMonthChangeResult) -> Unit)? = null

    /** Called for every successful user or programmatic date selection. */
    fun dateSelected(handler: (CalendarSelectionResult) -> Unit) {
        dateSelectedHandler = handler
    }

    /** Called after the visible month changes. */
    fun monthChanged(handler: (CalendarMonthChangeResult) -> Unit) {
        monthChangedHandler = handler
    }
}

/**
 * A polished, platform-independent Kuikly month calendar.
 *
 * The component owns its visible month and selection, while public methods allow
 * imperative control through a Kuikly `ViewRef`.
 */
class KuiklyCalendarView : ComposeView<CalendarAttr, CalendarEvent>() {
    var displayedMonth: CalendarMonth by observable(CalendarMonth(1970, 1))
        private set

    var selectedDate: CalendarDate? by observable(null)
        private set

    private var today: CalendarDate = CalendarDate(1970, 1, 1)

    override fun createAttr(): CalendarAttr = CalendarAttr()

    override fun createEvent(): CalendarEvent = CalendarEvent()

    override fun created() {
        super.created()
        today = CalendarPlatformDateTime.currentDate()
        val requestedSelection = attr.initialSelectedDate
            ?: today.takeIf { attr.selectTodayByDefault }
        selectedDate = requestedSelection?.takeIf(::isDateSelectable)

        val requestedMonth = attr.initialMonth
            ?: requestedSelection?.let { CalendarMonth(it.year, it.month) }
            ?: CalendarMonth(today.year, today.month)
        displayedMonth = clampMonthToRange(requestedMonth)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            View {
                attr {
                    backgroundColor(style.surfaceColor)
                    borderRadius(style.cornerRadius)
                    boxShadow(BoxShadow(0f, 10f, 28f, Color(0x1F334155L)))
                }

                apply(ctx.headerView())

                View {
                    attr {
                        padding(
                            top = 10f,
                            left = style.horizontalPadding,
                            bottom = if (ctx.attr.showFooter) 14f else 18f,
                            right = style.horizontalPadding,
                        )
                    }

                    apply(ctx.weekdayHeader())
                    apply(ctx.monthGrid())

                    vif({ ctx.attr.showFooter }) {
                        apply(ctx.footerView())
                    }
                }
            }
        }
    }

    /** Navigates to the previous available month. */
    fun previousMonth(): Boolean = navigateByMonths(
        offset = -1,
        source = CalendarNavigationSource.PROGRAMMATIC,
    )

    /** Navigates to the next available month. */
    fun nextMonth(): Boolean = navigateByMonths(
        offset = 1,
        source = CalendarNavigationSource.PROGRAMMATIC,
    )

    /** Navigates to [month] if it intersects the configured date range. */
    fun showMonth(
        month: CalendarMonth,
        source: CalendarNavigationSource = CalendarNavigationSource.PROGRAMMATIC,
    ): Boolean {
        if (!canDisplayMonth(month) || month == displayedMonth) {
            return false
        }
        val previousMonth = displayedMonth
        displayedMonth = month
        event.monthChangedHandler?.invoke(
            CalendarMonthChangeResult(previousMonth, month, source),
        )
        return true
    }

    /** Selects [date], navigating first when it belongs to another month. */
    fun selectDate(
        date: CalendarDate,
        source: CalendarNavigationSource = CalendarNavigationSource.PROGRAMMATIC,
    ): Boolean {
        if (!isDateSelectable(date)) {
            return false
        }
        val dateMonth = CalendarMonth(date.year, date.month)
        if (dateMonth != displayedMonth && !showMonth(dateMonth, source)) {
            return false
        }
        selectedDate = date
        event.dateSelectedHandler?.invoke(
            CalendarSelectionResult(
                date = date,
                timestampMillis = CalendarPlatformDateTime.startOfDayTimestampMillis(date),
                displayedMonth = displayedMonth,
                isToday = date == today,
                source = source,
            ),
        )
        return true
    }

    /** Navigates to today and selects it when enabled. */
    fun goToToday(): Boolean = goToToday(CalendarNavigationSource.PROGRAMMATIC)

    private fun goToToday(source: CalendarNavigationSource): Boolean {
        if (!isDateSelectable(today)) {
            return false
        }
        val todayMonth = CalendarMonth(today.year, today.month)
        if (!canDisplayMonth(todayMonth)) {
            return false
        }
        showMonth(todayMonth, source)
        return selectDate(today, source)
    }

    private fun headerView(): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            View {
                attr {
                    height(style.headerHeight)
                    padding(top = 18f, left = 18f, bottom = 16f, right = 18f)
                    borderRadius(
                        BorderRectRadius(
                            style.cornerRadius,
                            style.cornerRadius,
                            0f,
                            0f,
                        ),
                    )
                    backgroundLinearGradient(
                        Direction.TO_BOTTOM_RIGHT,
                        ColorStop(style.headerStartColor, 0f),
                        ColorStop(style.headerEndColor, 1f),
                    )
                }

                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                    }

                    apply(ctx.navigationButton(previous = true))

                    View {
                        attr {
                            flex(1f)
                            alignItemsCenter()
                        }
                        View {
                            attr {
                                height(28f)
                                alignSelfStretch()
                                allCenter()
                            }
                            Text {
                                attr {
                                    text(ctx.attr.locale.monthTitle(ctx.displayedMonth))
                                    color(style.headerTextColor)
                                    fontSize(22f)
                                    fontWeight600()
                                    lines(1)
                                    textAlignCenter()
                                }
                            }
                        }
                        Text {
                            attr {
                                marginTop(5f)
                                text(ctx.attr.locale.headerHint)
                                color(style.headerSecondaryTextColor)
                                fontSize(12f)
                            }
                        }
                    }

                    apply(ctx.navigationButton(previous = false))
                }
            }
        }
    }

    private fun navigationButton(previous: Boolean): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            View {
                attr {
                    val enabled = if (previous) ctx.canNavigatePrevious() else ctx.canNavigateNext()
                    size(40f, 40f)
                    allCenter()
                    borderRadius(20f)
                    backgroundColor(Color(0x24FFFFFFL))
                    opacity(if (enabled) 1f else 0.35f)
                    touchEnable(enabled)
                    accessibility(
                        if (previous) "Previous month" else "Next month",
                    )
                    accessibilityRole(AccessibilityRole.BUTTON)
                }
                event {
                    click {
                        if (previous) {
                            ctx.navigateByMonths(
                                -1,
                                CalendarNavigationSource.PREVIOUS_BUTTON,
                            )
                        } else {
                            ctx.navigateByMonths(
                                1,
                                CalendarNavigationSource.NEXT_BUTTON,
                            )
                        }
                    }
                }
                Canvas({
                    attr {
                        size(14f, 18f)
                    }
                }) { context, width, height ->
                    val direction = if (previous) -1f else 1f
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val horizontalRadius = 3.5f
                    val verticalRadius = 5.5f
                    context.beginPath()
                    context.moveTo(
                        centerX - direction * horizontalRadius,
                        centerY - verticalRadius,
                    )
                    context.lineTo(centerX + direction * horizontalRadius, centerY)
                    context.lineTo(
                        centerX - direction * horizontalRadius,
                        centerY + verticalRadius,
                    )
                    context.strokeStyle(style.headerTextColor)
                    context.lineWidth(2.4f)
                    context.lineCapRound()
                    context.stroke()
                }
            }
        }
    }

    private fun weekdayHeader(): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            vbind({ listOf(ctx.attr.weekStart, ctx.attr.locale) }) {
                val titles = ctx.attr.locale.weekdayTitles(ctx.attr.weekStart)
                View {
                    attr {
                        height(style.weekdayHeight)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    titles.forEach { title ->
                        View {
                            attr {
                                flex(1f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text(title)
                                    color(style.secondaryTextColor)
                                    fontSize(if (title.length > 1) 11f else 12f)
                                    fontWeight600()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun monthGrid(): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            vbind({
                listOf(
                    ctx.displayedMonth,
                    ctx.attr.weekStart,
                    ctx.attr.showAdjacentMonthDates,
                    ctx.attr.allowAdjacentMonthSelection,
                    ctx.attr.minDate,
                    ctx.attr.maxDate,
                    ctx.attr.dateEnabledPredicate,
                    ctx.attr.dateIndicatorPredicate,
                )
            }) {
                val grid = CalendarMath.buildMonthGrid(ctx.displayedMonth, ctx.attr.weekStart)
                View {
                    attr {
                        height(style.dayCellHeight * CalendarMonthGrid.WEEK_COUNT)
                    }
                    for (row in 0 until CalendarMonthGrid.WEEK_COUNT) {
                        View {
                            attr {
                                height(style.dayCellHeight)
                                flexDirectionRow()
                            }
                            grid.week(row).forEach { cell ->
                                apply(ctx.dayCell(cell))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dayCell(cell: CalendarDayCell): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        val isAdjacent = cell.relation != CalendarDayRelation.CURRENT_MONTH
        val isVisible = !isAdjacent || attr.showAdjacentMonthDates
        val enabled = isVisible && isDateSelectable(cell.date) &&
            (!isAdjacent || attr.allowAdjacentMonthSelection)
        val hasIndicator = isVisible && attr.dateIndicatorPredicate(cell.date)
        val isWeekend = cell.dayOfWeek == CalendarDayOfWeek.SUNDAY ||
            cell.dayOfWeek == CalendarDayOfWeek.SATURDAY

        return {
            View {
                attr {
                    flex(1f)
                    height(style.dayCellHeight)
                    allCenter()
                    touchEnable(enabled)
                    accessibility(ctx.dayAccessibility(cell.date, enabled))
                    accessibilityRole(AccessibilityRole.BUTTON)
                }
                event {
                    click {
                        ctx.selectDate(
                            cell.date,
                            if (isAdjacent) {
                                CalendarNavigationSource.ADJACENT_DATE
                            } else {
                                CalendarNavigationSource.DATE_CELL
                            },
                        )
                    }
                }

                View {
                    attr {
                        val selected = ctx.selectedDate == cell.date
                        val isToday = ctx.today == cell.date
                        size(style.dayBubbleSize, style.dayBubbleSize)
                        allCenter()
                        borderRadius(style.dayBubbleSize / 2f)
                        backgroundColor(if (selected) style.primaryColor else Color.TRANSPARENT)
                        border(
                            if (isToday && !selected) {
                                Border(1.5f, BorderStyle.SOLID, style.primaryColor)
                            } else {
                                Border(0f, BorderStyle.SOLID, Color.TRANSPARENT)
                            },
                        )
                        transform(scale = Scale(if (selected) 1f else 0.96f))
                        animation(Animation.easeOut(0.18f), selected)
                    }

                    Text {
                        attr {
                            val selected = ctx.selectedDate == cell.date
                            text(if (isVisible) cell.date.day.toString() else "")
                            color(
                                when {
                                    selected -> Color.WHITE
                                    !enabled -> style.disabledDateTextColor
                                    isAdjacent -> style.adjacentDateTextColor
                                    isWeekend -> style.weekendTextColor
                                    else -> style.dateTextColor
                                },
                            )
                            fontSize(14f)
                            if (selected || ctx.today == cell.date) {
                                fontWeight600()
                            } else {
                                fontWeight400()
                            }
                        }
                    }

                    if (hasIndicator) {
                        View {
                            attr {
                                val selected = ctx.selectedDate == cell.date
                                size(4f, 4f)
                                borderRadius(2f)
                                backgroundColor(
                                    if (selected) Color.WHITE else style.indicatorColor,
                                )
                                absolutePosition(bottom = 3f, left = 16f)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun footerView(): ViewBuilder {
        val ctx = this
        val style = attr.calendarStyle
        return {
            View {
                attr {
                    marginTop(10f)
                    padding(top = 13f)
                    border(Border(0f, BorderStyle.SOLID, Color.TRANSPARENT))
                }

                View {
                    attr {
                        height(1f)
                        backgroundColor(style.dividerColor)
                        absolutePosition(top = 0f, left = 0f, right = 0f)
                    }
                }

                View {
                    attr {
                        minHeight(34f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }

                    Text {
                        attr {
                            flex(1f)
                            val selected = ctx.selectedDate
                            text(
                                if (selected == null) {
                                    ctx.attr.locale.emptySelectionText
                                } else {
                                    "${ctx.attr.locale.selectedPrefix}  ${selected.toIsoDate()}"
                                },
                            )
                            color(style.secondaryTextColor)
                            fontSize(12f)
                        }
                    }

                    View {
                        attr {
                            val enabled = ctx.isDateSelectable(ctx.today)
                            height(32f)
                            padding(left = 13f, right = 13f)
                            allCenter()
                            borderRadius(16f)
                            backgroundColor(style.primarySoftColor)
                            opacity(if (enabled) 1f else 0.4f)
                            touchEnable(enabled)
                            accessibility(ctx.attr.locale.todayText)
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.goToToday(CalendarNavigationSource.TODAY_BUTTON)
                            }
                        }
                        Text {
                            attr {
                                text(ctx.attr.locale.todayText)
                                color(style.primaryColor)
                                fontSize(12f)
                                fontWeight600()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun canNavigatePrevious(): Boolean = adjacentMonth(-1)?.let(::canDisplayMonth) == true

    private fun canNavigateNext(): Boolean = adjacentMonth(1)?.let(::canDisplayMonth) == true

    private fun navigateByMonths(
        offset: Int,
        source: CalendarNavigationSource,
    ): Boolean = adjacentMonth(offset)?.let { showMonth(it, source) } ?: false

    private fun adjacentMonth(offset: Int): CalendarMonth? {
        if (offset < 0 && displayedMonth.year == 1 && displayedMonth.month == 1) {
            return null
        }
        if (
            offset > 0 &&
            displayedMonth.year == Int.MAX_VALUE &&
            displayedMonth.month == 12
        ) {
            return null
        }
        return displayedMonth.plusMonths(offset)
    }

    private fun canDisplayMonth(month: CalendarMonth): Boolean {
        if (month.year !in CALENDAR_VIEW_YEAR_RANGE) {
            return false
        }
        val firstDate = CalendarDate(month.year, month.month, 1)
        val lastDate = CalendarDate(
            month.year,
            month.month,
            CalendarMath.daysInMonth(month.year, month.month),
        )
        return (attr.minDate == null || lastDate >= attr.minDate!!) &&
            (attr.maxDate == null || firstDate <= attr.maxDate!!)
    }

    private fun clampMonthToRange(month: CalendarMonth): CalendarMonth {
        val minMonth = attr.minDate?.let { CalendarMonth(it.year, it.month) }
        val maxMonth = attr.maxDate?.let { CalendarMonth(it.year, it.month) }
        val rangeClampedMonth = when {
            minMonth != null && month < minMonth -> minMonth
            maxMonth != null && month > maxMonth -> maxMonth
            else -> month
        }
        return when {
            rangeClampedMonth.year < CALENDAR_VIEW_YEAR_RANGE.first ->
                CalendarMonth(CALENDAR_VIEW_YEAR_RANGE.first, 1)
            rangeClampedMonth.year > CALENDAR_VIEW_YEAR_RANGE.last ->
                CalendarMonth(CALENDAR_VIEW_YEAR_RANGE.last, 12)
            else -> rangeClampedMonth
        }
    }

    private fun isDateSelectable(date: CalendarDate): Boolean {
        if (date.year !in CALENDAR_VIEW_YEAR_RANGE) {
            return false
        }
        if (attr.minDate != null && date < attr.minDate!!) {
            return false
        }
        if (attr.maxDate != null && date > attr.maxDate!!) {
            return false
        }
        return attr.dateEnabledPredicate(date)
    }

    private fun dayAccessibility(date: CalendarDate, enabled: Boolean): String {
        val states = buildList {
            if (date == today) add("today")
            if (date == selectedDate) add("selected")
            if (!enabled) add("disabled")
        }
        return if (states.isEmpty()) {
            date.toIsoDate()
        } else {
            "${date.toIsoDate()}, ${states.joinToString()}"
        }
    }
}

/** Adds a [KuiklyCalendarView] to a Kuikly view tree. */
fun ViewContainer<*, *>.Calendar(init: KuiklyCalendarView.() -> Unit) {
    addChild(KuiklyCalendarView(), init)
}

private val CALENDAR_VIEW_YEAR_RANGE = 1900..9998

private fun CalendarDate.toIsoDate(): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
