package com.rajratna.events.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date/time utility functions used throughout the app.
 */
object DateUtils {

    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val shortFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatDate(timestamp: Long): String = displayFormat.format(Date(timestamp))
    fun formatShortDate(timestamp: Long): String = shortFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = "${formatDate(timestamp)} ${formatTime(timestamp)}"

    /** Returns start of today (midnight) in millis. */
    fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Returns start of day (midnight) for any timestamp in millis. */
    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Returns end of today (just before midnight) in millis. */
    fun endOfToday(): Long = startOfToday() + 24 * 60 * 60 * 1000L

    /** Returns end of day (just before midnight) for any timestamp in millis. */
    fun endOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /** Returns start of tomorrow. */
    fun startOfTomorrow(): Long = startOfToday() + 24 * 60 * 60 * 1000L

    /** Returns end of tomorrow. */
    fun endOfTomorrow(): Long = startOfTomorrow() + 24 * 60 * 60 * 1000L

    /** Returns start of this week (Monday). */
    fun startOfThisWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // If today is Sunday and Monday is considered start, go back
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    /** Returns end of this week (Sunday night). */
    fun endOfThisWeek(): Long = startOfThisWeek() + 7 * 24 * 60 * 60 * 1000L

    /** Returns start of this month. */
    fun startOfThisMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Returns end of this month. */
    fun endOfThisMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis + 1
    }

    /** Calculate rental days between two dates (inclusive). */
    fun calculateRentalDays(deliveryDate: Long, returnDate: Long): Int {
        val diff = returnDate - deliveryDate
        val days = (diff / (24 * 60 * 60 * 1000L)).toInt()
        return if (days < 1) 1 else days
    }

    // ── Marathi Date Formatting ─────────────────────────────

    private val marathiMonths = arrayOf(
        "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल",
        "मे", "जून", "जुलै", "ऑगस्ट",
        "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
    )

    /**
     * Format timestamp as Marathi date string, e.g. "21 मे 2026".
     */
    fun formatMarathiDate(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = marathiMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }
}

/**
 * Format amount with ₹ symbol.
 */
fun Double.toRupee(): String = "₹${String.format("%,.0f", this)}"

/**
 * Format amount with ₹ symbol and decimals.
 */
fun Double.toRupeeDecimal(): String = "₹${String.format("%,.2f", this)}"
