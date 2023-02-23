package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.ui.helpers.StringExtension.capitalize
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@NablaInternal
public object DateFormattingExtension {
    /**
     * 24 févr., 15:32
     */
    @NablaInternal
    public fun Date.toFormattedRelativeWeekDayAndShortTimeString(context: Context): String {
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
     * 24/02/2022
     */
    @NablaInternal
    public fun Date.toFormattedNumericDate(context: Context): String {
        return DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_NUMERIC_DATE
        )
    }

    /**
     * 24 févr.
     */
    @NablaInternal
    public fun Date.toFormattedDayOfMonth(context: Context): String {
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
    @NablaInternal
    public fun Date.toFormattedTime(context: Context): String {
        return DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_TIME
        )
    }

    /**
     * lun.
     */
    @NablaInternal
    public fun Date.toFormattedShortWeekDay(context: Context): String {
        return DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_ABBREV_WEEKDAY
        ).capitalize(Locale.getDefault())
    }
}
