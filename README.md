# Kuikly Calendar View

一个基于 Kuikly `ComposeView` 的现代月份日历组件。组件与日期算法全部位于
`commonMain`，同一套 Kotlin API 可运行在 Android、iOS、Web 与 HarmonyOS。

Demo 页面名为 `calendar_demo`，当前 Android、iOS、HarmonyOS 宿主均默认打开该页面。

## 功能

- 当前月份标题，以及上一月、下一月和“回到今天”快捷切换
- 稳定的 `6 × 7` 日期网格、星期表头、周日/周一起始配置
- 当前日描边、选中动画、周末色、相邻月日期、禁用态与事件圆点
- 点击选择日期，支持点击相邻月日期后自动切月
- 最小/最大日期范围和自定义日期可用规则
- `dateSelected`、`monthChanged` 完整类型化回调
- 选择结果返回结构化日期、ISO 日期、本地当日开始毫秒时间戳与触发来源
- 中英文预设、本地化扩展、完整主题 token 与无障碍描述
- 纯 Kotlin Gregorian 日期算法，不依赖 `java.time`

## 快速使用

```kotlin
import com.guet.liang.kuiklycalendarview.calendar.Calendar
import com.guet.liang.kuiklycalendarview.calendar.CalendarDate
import com.guet.liang.kuiklycalendarview.calendar.CalendarWeekStart

Calendar {
    attr {
        initialSelectedDate(CalendarDate(2026, 7, 28))
        dateRange(
            minDate = CalendarDate(2025, 1, 1),
            maxDate = CalendarDate(2027, 12, 31),
        )
        weekStart(CalendarWeekStart.MONDAY)
        showAdjacentMonthDates(true)
        allowAdjacentMonthSelection(true)
        dateEnabled { date -> date.day != 8 }
        dateIndicator { date -> date.day in listOf(4, 12, 19) }
    }
    event {
        dateSelected { result ->
            println(result.date)
            println(result.isoDate)
            println(result.timestampMillis)
            println(result.source)
        }
        monthChanged { result ->
            println("${result.previousMonth} -> ${result.month}")
            println(result.source)
        }
    }
}
```

未配置 `initialMonth` 时显示设备本地当前月份；未配置初始选择时默认选中今天。
`timestampMillis` 明确定义为设备本地时区所选日期的首个有效时刻（通常为
`00:00:00.000`；若 DST 跳过午夜，则为系统归一化后的当日开始时刻）。
组件使用各平台本地时钟实现该语义，不依赖各端格式化行为不完全一致的
`CalendarModule.formatTime`。
UI 组件支持 `1900..9998` 年，确保固定 42 格始终可以安全包含前后相邻月份；
纯算法模型仍支持更广的 Gregorian 年份。

## Attr API

| API | 默认值 | 说明 |
| --- | --- | --- |
| `initialMonth(CalendarMonth)` | 本地当前月 | 初次展示的月份 |
| `initialSelectedDate(CalendarDate?)` | 今天 | 初始选中日期 |
| `dateRange(minDate, maxDate)` | 无限制 | 闭区间日期范围，同时限制翻月 |
| `weekStart(CalendarWeekStart)` | `SUNDAY` | 星期表头从周日或周一开始 |
| `showAdjacentMonthDates(Boolean)` | `true` | 是否显示首尾的相邻月日期 |
| `allowAdjacentMonthSelection(Boolean)` | `true` | 相邻月日期是否可点击并切月 |
| `showFooter(Boolean)` | `true` | 是否显示选择摘要和今天按钮 |
| `selectTodayByDefault(Boolean)` | `true` | 无初始日期时是否选中今天 |
| `locale(CalendarLocale)` | `ZH_CN` | 文案、星期与月份标题格式 |
| `style(CalendarStyle)` | 现代紫色主题 | 颜色、圆角、间距与尺寸 token |
| `dateEnabled { date -> ... }` | 全部可用 | 自定义业务禁用规则 |
| `dateIndicator { date -> ... }` | 全部无标记 | 自定义事件圆点规则 |

所有配置通过 Kuikly 标准 `attr {}` DSL 提供，不依赖任一宿主平台 API。`initial*`、
主题、范围和业务 predicate 属于创建期配置；运行时月份与日期控制请使用下方命令式 API。

## Event API

### `dateSelected`

回调参数为 `CalendarSelectionResult`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `date` | `CalendarDate` | 年、月、日结构化结果 |
| `isoDate` | `String` | `yyyy-MM-dd` |
| `dayOfWeek` | `CalendarDayOfWeek` | 选中日期对应的星期 |
| `timestampMillis` | `Long` | 设备本地日历的当日开始时间戳 |
| `displayedMonth` | `CalendarMonth` | 选择完成后的可见月份 |
| `isToday` | `Boolean` | 是否为设备本地今天 |
| `source` | `CalendarNavigationSource` | 日期网格、相邻月或今天按钮等来源 |

### `monthChanged`

回调参数为 `CalendarMonthChangeResult`，包含 `previousMonth`、`month` 和 `source`。
达到 `minDate` / `maxDate` 边界时导航按钮自动禁用，不会产生无效回调。

## 命令式 API

通过 Kuikly `ViewRef<KuiklyCalendarView>` 可调用：

```kotlin
lateinit var calendarRef: ViewRef<KuiklyCalendarView>

Calendar {
    ref { calendarRef = it }
}

calendarRef.view?.previousMonth()
calendarRef.view?.nextMonth()
calendarRef.view?.showMonth(CalendarMonth(2026, 10))
calendarRef.view?.selectDate(CalendarDate(2026, 10, 1))
calendarRef.view?.goToToday()
```

每个方法返回 `Boolean`，表示操作是否成功；成功操作使用与用户点击相同的事件回调。

## 本地化与主题

组件内置 `CalendarLocale.ZH_CN` 与 `CalendarLocale.EN_US`。也可提供自定义星期文案、
按钮文案和月份标题 formatter：

```kotlin
attr {
    locale(CalendarLocale.EN_US)
    style(
        CalendarStyle(
            headerStartColor = Color(0xFF0F766EL),
            headerEndColor = Color(0xFF14B8A6L),
            primaryColor = Color(0xFF0F766EL),
        ),
    )
}
```

日期点击区为 `44 × 44`，选中态带轻量动画；月份按钮、日期格和今天按钮均提供
`accessibility` 描述及按钮角色。

## 日期算法

公开的 `CalendarMath` 提供：

- `isLeapYear(year)`
- `daysInMonth(year, month)`
- `dayOfWeek(date)`
- `buildMonthGrid(month, weekStart)`

`buildMonthGrid` 始终返回 42 个连续的 `CalendarDayCell`，每格包含日期、星期、
所在行列和 `PREVIOUS_MONTH / CURRENT_MONTH / NEXT_MONTH` 关系。算法为纯 Gregorian
实现，可在业务逻辑和单元测试中脱离 UI 使用。

## Demo

完整示例位于
`shared/src/commonMain/kotlin/com/guet/liang/kuiklycalendarview/CalendarDemoPage.kt`。

Demo 展示：

- 月份前后切换与今天快捷入口
- 周一起始表头和相邻月日期选择
- 选中、今天、周末、禁用和事件标记视觉状态
- `dateSelected` 与 `monthChanged` 实时回调结果
- Android、iOS、Web、HarmonyOS 共用页面实现

## 运行与验证

### Android

```bash
./gradlew :androidApp:assembleDebug
```

### iOS

使用 Xcode 打开 `iosApp/iosApp.xcworkspace`，运行 `iosApp` scheme。

### Web

```bash
./gradlew :shared:compileKotlinJs
```

该命令验证组件和 Demo 的 JS 目标。当前 starter 没有提供可直接启动的 `h5App` 宿主；
生成业务包后需按 Kuikly Web 宿主接入方式加载。Android、iOS、HarmonyOS 宿主已包含在仓库中。

### HarmonyOS

使用项目的 HarmonyOS settings 构建 shared 产物，然后在 DevEco Studio 运行 `ohosApp`：

```bash
./gradlew -c settings.ohos.gradle.kts :shared:compileKotlinOhosArm64
```

### 测试

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinJs
./gradlew :shared:compileKotlinIosSimulatorArm64
```

## 目录

```text
shared/src/commonMain/kotlin/com/guet/liang/kuiklycalendarview/
├── CalendarDemoPage.kt
└── calendar/
    ├── CalendarModels.kt
    ├── CalendarPlatformDateTime.kt
    └── KuiklyCalendarView.kt

shared/src/{androidMain,iosMain,jsMain,ohosArm64Main}/.../calendar/
└── CalendarPlatformDateTime.*.kt

shared/src/commonTest/kotlin/com/guet/liang/kuiklycalendarview/calendar/
└── CalendarModelsTest.kt
```
