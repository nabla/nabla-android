package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private fun Date.roundedToDayStart(): Date {
    Calendar.getInstance().apply {
        time = this@roundedToDayStart
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        return time
    }
}

/**
 * 24 févr., 15:32
 */
fun Date.toFormattedRelativeWeekDayAndShortTimeString(context: Context): String {
    return if (abs(Date().time - time) > DAY_IN_MILLIS * 2) {
        DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_SHOW_TIME or
                @Suppress("DEPRECATION") DateUtils.FORMAT_24HOUR,
        )
    } else {
        DateUtils.getRelativeDateTimeString(
            context,
            time,
            DAY_IN_MILLIS,
            DAY_IN_MILLIS * 2,
            DateUtils.FORMAT_SHOW_DATE
        ).toString()
    }.capitalize(Locale.getDefault())
}

/**
 * 24 févr. 2022
 */
fun Date.toFormattedShortDate(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_YEAR,
    )
}

/**
 * 24/02/2022
 */
fun Date.toFormattedNumericDate(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_NUMERIC_DATE
    )
}

/**
 * 24 févr.
 */
fun Date.toFormattedDayOfMonth(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_NO_YEAR,
    )
}

/**
 * 15:32 or 03:32
 */
fun Date.toFormattedTime(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_TIME
    )
}

/**
 * 15:32
 */
fun Date.toFormattedTime24h(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_TIME or
            @Suppress("DEPRECATION") DateUtils.FORMAT_24HOUR
    )
}

/**
 * Lundi 24 Février
 */
fun Date.toFormattedWeekDayAndMonth(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_DATE
    ).capitalize(Locale.getDefault())
}

/**
 * lun.
 */
fun Date.toFormattedShortWeekDay(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_ABBREV_WEEKDAY
    ).capitalize(Locale.getDefault())
}

/**
 * Lundi 23 Février 2022
 */
fun Date.toFormattedWeekDayAndMonthAndYear(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_YEAR
    ).capitalize(Locale.getDefault())
}

/**
 * Février
 */
fun Date.toFormattedMonth(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_NO_MONTH_DAY or
            DateUtils.FORMAT_NO_YEAR
    ).capitalize(Locale.getDefault())
}

/**
 * Février 2022
 */
fun Date.toFormattedMonthAndYear(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        time,
        DateUtils.FORMAT_NO_MONTH_DAY or
            DateUtils.FORMAT_SHOW_YEAR
    ).capitalize(Locale.getDefault())
}

private fun Date.toYear(): Int = with(Calendar.getInstance()) {
    time = this@toYear
    get(Calendar.YEAR)
}

/**
 * 2022
 */
fun Date.toFormattedYear(): String {
    return with(Calendar.getInstance()) {
        time = this@toFormattedYear
        get(Calendar.YEAR).toString()
    }
}
