package com.nabla.sdk.core.ui.helpers

import java.util.Calendar
import java.util.Date

fun Date.roundedToMonthStart(): Date {
    Calendar.getInstance().apply {
        time = this@roundedToMonthStart
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_MONTH, 1)
        return time
    }
}

fun Date.minusDays(daysCount: Int) = plusDays(-daysCount)

fun Date.plusDays(daysCount: Int): Date {
    Calendar.getInstance().apply {
        time = this@plusDays
        add(Calendar.DAY_OF_MONTH, daysCount)
        return time
    }
}

fun Date.plusMinutes(count: Int): Date {
    Calendar.getInstance().apply {
        time = this@plusMinutes
        add(Calendar.MINUTE, count)
        return time
    }
}

fun Date.plusSeconds(count: Int): Date {
    Calendar.getInstance().apply {
        time = this@plusSeconds
        add(Calendar.SECOND, count)
        return time
    }
}

fun Date.plusMillis(count: Int): Date {
    Calendar.getInstance().apply {
        time = this@plusMillis
        add(Calendar.MILLISECOND, count)
        return time
    }
}

fun Date.isSameYearAs(other: Date): Boolean {
    val thisCalendar = Calendar.getInstance().apply { time = this@isSameYearAs }
    val otherCalendar = Calendar.getInstance().apply { time = other }
    return thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
}

fun Date.isSameDayAndYearAs(other: Date): Boolean {
    val thisCalendar = Calendar.getInstance().apply { time = this@isSameDayAndYearAs }
    val otherCalendar = Calendar.getInstance().apply { time = other }
    return thisCalendar.get(Calendar.DAY_OF_YEAR) == otherCalendar.get(Calendar.DAY_OF_YEAR) &&
        thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
}

fun Date.isToday(): Boolean {
    return isSameDayAndYearAs(Date())
}

fun Date.isThisYear(): Boolean {
    return isSameYearAs(Date())
}

fun Date.isThisWeek(): Boolean {
    val now = Calendar.getInstance()
    val other = Calendar.getInstance().apply { time = this@isThisWeek }
    return other.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
        other.get(Calendar.YEAR) == now.get(Calendar.YEAR)
}
