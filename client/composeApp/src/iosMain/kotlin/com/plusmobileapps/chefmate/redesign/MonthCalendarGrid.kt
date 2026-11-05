package com.plusmobileapps.chefmate.redesign

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * One cell of a month grid. A padding cell before the 1st or after the last day has a null [date]
 * and [day] 0. [key] is the ISO date (`2026-11-04`), the format the meal BLoCs use for
 * `daysWithMeals`; empty for padding.
 */
class CalendarCell(val date: LocalDate?, val day: Int, val key: String)

/**
 * The month grid the Compose meal calendars draw (Sunday-first weeks), for the SwiftUI redesign's
 * meal plan and choose-date screens. Kept in Kotlin so both UIs lay a month out the same way, and
 * so Swift never has to build a `LocalDate` itself.
 */
object MonthCalendarGrid {
    /** [firstDayOfMonth]'s month as whole weeks of 7 cells, padded at both ends. */
    fun cells(firstDayOfMonth: LocalDate): List<CalendarCell> {
        val first = LocalDate(firstDayOfMonth.year, firstDayOfMonth.month, 1)
        val daysInMonth = first.plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1)).day
        // DayOfWeek runs Monday (0) … Sunday (6); shift it so Sunday leads the week.
        val leading = (first.dayOfWeek.ordinal + 1) % 7
        val total = (leading + daysInMonth + 6) / 7 * 7
        return List(total) { index ->
            val day = index - leading + 1
            if (day in 1..daysInMonth) {
                val date = LocalDate(first.year, first.month, day)
                CalendarCell(date = date, day = day, key = date.toString())
            } else {
                CalendarCell(date = null, day = 0, key = "")
            }
        }
    }

    /** Single-letter weekday headers, Sunday first, matching the Compose calendars. */
    val weekdayInitials: List<String> =
        listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            )
            .map { it.name.first().toString() }

    /** [date]'s grid key (its ISO string), to compare with [CalendarCell.key]; null for null. */
    fun key(date: LocalDate?): String? = date?.toString()
}
