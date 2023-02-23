package com.nabla.sdk.core.ui.helpers

import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.datetime.Instant
import java.util.Calendar
import java.util.Date

@NablaInternal
public object DateParsingExtension {
    @NablaInternal
    public fun Instant.toJavaDate(): Date = Date(toEpochMilliseconds())

    @NablaInternal
    public fun Date.isSameYearAs(other: Date): Boolean {
        val thisCalendar = Calendar.getInstance().apply { time = this@isSameYearAs }
        val otherCalendar = Calendar.getInstance().apply { time = other }
        return thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
    }

    @NablaInternal
    public fun Date.isSameDayAndYearAs(other: Date): Boolean {
        val thisCalendar = Calendar.getInstance().apply { time = this@isSameDayAndYearAs }
        val otherCalendar = Calendar.getInstance().apply { time = other }
        return thisCalendar.get(Calendar.DAY_OF_YEAR) == otherCalendar.get(Calendar.DAY_OF_YEAR) &&
            thisCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
    }

    @NablaInternal
    public fun Date.isToday(): Boolean {
        return isSameDayAndYearAs(Date())
    }

    @NablaInternal
    public fun Date.isThisYear(): Boolean {
        return isSameYearAs(Date())
    }

    @NablaInternal
    public fun Date.isThisWeek(): Boolean {
        val now = Calendar.getInstance()
        val other = Calendar.getInstance().apply { time = this@isThisWeek }
        return other.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
            other.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }
}
