package fr.croustillapp.data.helper

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)

fun isToday(dateStr: String): Boolean = compareDateWithOffset(dateStr, 0)

fun isTomorrow(dateStr: String): Boolean = compareDateWithOffset(dateStr, 1)

private fun compareDateWithOffset(dateStr: String, dayOffset: Int): Boolean {
    return try {
        val targetDate = dateFormatter.parse(dateStr) ?: return false

        val comparisonDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        targetDate == comparisonDate
    } catch (_: Exception) {
        false
    }
}