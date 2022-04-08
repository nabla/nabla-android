package com.nabla.sdk.core.ui.helpers

import kotlinx.datetime.Instant
import java.util.Calendar
import java.util.Date

internal fun Instant.toJavaDate() = Date(toEpochMilliseconds())

internal fun Date.isSameYearAs(other: Date): Boolean {
    val thisCalendar = Calendar.getInstance().apply { time = this@isSameYearAs }
    val otherCalendar = Calendar.getInstance().apply { time = other }
    return thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
}

internal fun Date.isSameDayAndYearAs(other: Date): Boolean {
    val thisCalendar = Calendar.getInstance().apply { time = this@isSameDayAndYearAs }
    val otherCalendar = Calendar.getInstance().apply { time = other }
    return thisCalendar.get(Calendar.DAY_OF_YEAR) == otherCalendar.get(Calendar.DAY_OF_YEAR) &&
        thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
}

internal fun Date.isToday(): Boolean {
    return isSameDayAndYearAs(Date())
}

internal fun Date.isThisYear(): Boolean {
    return isSameYearAs(Date())
}

internal fun Date.isThisWeek(): Boolean {
    val now = Calendar.getInstance()
    val other = Calendar.getInstance().apply { time = this@isThisWeek }
    return other.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
        other.get(Calendar.YEAR) == now.get(Calendar.YEAR)
}
