package com.guet.liang.kuiklycalendarview

import com.guet.liang.kuiklycalendarview.base.BasePager
import com.guet.liang.kuiklycalendarview.calendar.Calendar
import com.guet.liang.kuiklycalendarview.calendar.CalendarDate
import com.guet.liang.kuiklycalendarview.calendar.CalendarMonthChangeResult
import com.guet.liang.kuiklycalendarview.calendar.CalendarNavigationSource
import com.guet.liang.kuiklycalendarview.calendar.CalendarSelectionResult
import com.guet.liang.kuiklycalendarview.calendar.CalendarWeekStart
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("calendar_demo", supportInLocal = true)
internal class CalendarDemoPage : BasePager() {
    private var selectedDateText: String by observable("等待日期选择")
    private var selectedTimestampText: String by observable("timestampMillis 将在这里返回")
    private var monthChangedText: String by observable("使用左右按钮切换月份")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(BACKGROUND_COLOR)
            }

            View {
                attr {
                    padding(
                        top = pagerData.statusBarHeight + 18f,
                        left = 22f,
                        bottom = 18f,
                        right = 22f,
                    )
                }
                Text {
                    attr {
                        text("Kuikly Calendar")
                        color(TITLE_COLOR)
                        fontSize(28f)
                        fontWeight700()
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        text("一套声明式 API，四端一致的日期体验")
                        color(SECONDARY_TEXT_COLOR)
                        fontSize(14f)
                    }
                }
                View {
                    attr {
                        marginTop(14f)
                        flexDirectionRow()
                    }
                    listOf("Android", "iOS", "Web", "HarmonyOS").forEach { platform ->
                        apply(ctx.platformPill(platform))
                    }
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    showScrollerIndicator(false)
                    bouncesEnable(true)
                    padding(
                        left = 18f,
                        right = 18f,
                        bottom = pagerData.safeAreaInsets.bottom + 28f,
                    )
                }

                Calendar {
                    attr {
                        weekStart(CalendarWeekStart.MONDAY)
                        showAdjacentMonthDates(true)
                        allowAdjacentMonthSelection(true)
                        showFooter(true)
                        selectTodayByDefault(true)
                        dateEnabled { date ->
                            date.day != 8 && date.day != 21
                        }
                        dateIndicator { date ->
                            date.day == 4 || date.day == 12 || date.day == 19
                        }
                    }
                    event {
                        dateSelected(ctx::handleDateSelected)
                        monthChanged(ctx::handleMonthChanged)
                    }
                }

                View {
                    attr {
                        marginTop(18f)
                        padding(top = 18f, left = 18f, bottom = 18f, right = 18f)
                        backgroundColor(Color.WHITE)
                        borderRadius(18f)
                        boxShadow(BoxShadow(0f, 6f, 20f, Color(0x14334155L)))
                    }
                    Text {
                        attr {
                            text("交互状态")
                            color(TITLE_COLOR)
                            fontSize(16f)
                            fontWeight600()
                        }
                    }

                    apply(
                        ctx.callbackRow(
                            label = "dateSelected",
                            value = { ctx.selectedDateText },
                            detail = { ctx.selectedTimestampText },
                            color = Color(0xFF635BFFL),
                        ),
                    )
                    apply(
                        ctx.callbackRow(
                            label = "monthChanged",
                            value = { ctx.monthChangedText },
                            detail = { "回调包含前后月份与触发来源" },
                            color = Color(0xFF0EA5E9L),
                        ),
                    )
                }

                View {
                    attr {
                        marginTop(14f)
                        padding(top = 16f, left = 18f, bottom = 18f, right = 18f)
                        backgroundColor(Color.WHITE)
                        borderRadius(18f)
                    }
                    Text {
                        attr {
                            text("图例与体验细节")
                            color(TITLE_COLOR)
                            fontSize(15f)
                            fontWeight600()
                        }
                    }
                    View {
                        attr {
                            marginTop(14f)
                            flexDirectionRow()
                            justifyContentSpaceBetween()
                        }
                        apply(ctx.legendItem(Color(0xFF635BFFL), "选中"))
                        apply(ctx.legendItem(Color(0xFFF59E0BL), "有事件"))
                        apply(ctx.legendItem(Color(0xFFF97316L), "周末"))
                        apply(ctx.legendItem(Color(0xFFE2E8F0L), "不可选"))
                    }
                    Text {
                        attr {
                            marginTop(14f)
                            text("可点击相邻月份日期直接跳转；每月 8 日、21 日演示禁用态，4 日、12 日、19 日演示事件标记。")
                            color(SECONDARY_TEXT_COLOR)
                            fontSize(12f)
                            lineHeight(19f)
                        }
                    }
                }
            }
        }
    }

    private fun handleDateSelected(result: CalendarSelectionResult) {
        selectedDateText = buildString {
            append(result.isoDate)
            if (result.isToday) {
                append(" · 今天")
            }
            append(" · ")
            append(result.source.displayName())
        }
        selectedTimestampText = "本地当日开始时间戳：${result.timestampMillis}"
    }

    private fun handleMonthChanged(result: CalendarMonthChangeResult) {
        monthChangedText =
            "${result.previousMonth} → ${result.month} · ${result.source.displayName()}"
    }

    private fun platformPill(platform: String): ViewBuilder = {
        View {
            attr {
                marginRight(8f)
                height(26f)
                padding(left = 10f, right = 10f)
                allCenter()
                backgroundColor(Color(0xFFEDE9FEL))
                borderRadius(13f)
            }
            Text {
                attr {
                    text(platform)
                    color(Color(0xFF6D5CE7L))
                    fontSize(10f)
                    fontWeight600()
                }
            }
        }
    }

    private fun callbackRow(
        label: String,
        value: () -> String,
        detail: () -> String,
        color: Color,
    ): ViewBuilder = {
        View {
            attr {
                marginTop(14f)
                padding(top = 12f, left = 12f, bottom = 12f, right = 12f)
                backgroundColor(Color(0xFFF8FAFCL))
                borderRadius(12f)
                border(Border(1f, BorderStyle.SOLID, Color(0xFFEFF2F7L)))
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                View {
                    attr {
                        size(7f, 7f)
                        borderRadius(3.5f)
                        backgroundColor(color)
                    }
                }
                Text {
                    attr {
                        marginLeft(8f)
                        text(label)
                        color(color)
                        fontSize(11f)
                        fontWeight600()
                    }
                }
            }
            Text {
                attr {
                    marginTop(8f)
                    text(value())
                    color(TITLE_COLOR)
                    fontSize(13f)
                    fontWeight500()
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    text(detail())
                    color(SECONDARY_TEXT_COLOR)
                    fontSize(11f)
                }
            }
        }
    }

    private fun legendItem(color: Color, text: String): ViewBuilder = {
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
            }
            View {
                attr {
                    size(9f, 9f)
                    borderRadius(4.5f)
                    backgroundColor(color)
                }
            }
            Text {
                attr {
                    marginLeft(5f)
                    text(text)
                    color(SECONDARY_TEXT_COLOR)
                    fontSize(11f)
                }
            }
        }
    }

    private fun CalendarNavigationSource.displayName(): String = when (this) {
        CalendarNavigationSource.PREVIOUS_BUTTON -> "上一月按钮"
        CalendarNavigationSource.NEXT_BUTTON -> "下一月按钮"
        CalendarNavigationSource.DATE_CELL -> "日期网格"
        CalendarNavigationSource.ADJACENT_DATE -> "相邻月日期"
        CalendarNavigationSource.TODAY_BUTTON -> "今天按钮"
        CalendarNavigationSource.PROGRAMMATIC -> "命令式 API"
    }

    companion object {
        private val BACKGROUND_COLOR = Color(0xFFF4F6FBL)
        private val TITLE_COLOR = Color(0xFF172033L)
        private val SECONDARY_TEXT_COLOR = Color(0xFF64748BL)
    }
}
