package fr.croustillapp.data.helper

import fr.croustillapp.R
import java.time.LocalDate

object HolidayHelper {

    private val droitLocalZones = setOf(
        "Strasbourg", "Colmar", "Illkirch-Graffenstaden", "Mulhouse", "Erstein", "Saint-Louis", "Saverne", "Sélestat",
        "Metz", "Yutz", "Metz Technopole"
    )


    fun isDroitLocalZone(zone: String): Boolean {
        return droitLocalZones.any { it.equals(zone, ignoreCase = true) }
    }

    private fun getEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.of(year, month, day)
    }

    fun getHolidayNameRes(date: LocalDate, isDroitLocal: Boolean): Int? {
        val day = date.dayOfMonth
        val month = date.monthValue
        val year = date.year

        when {
            day == 1 && month == 1 -> return R.string.ferie_nouvelAn
            day == 1 && month == 5 -> return R.string.ferie_premierMai
            day == 8 && month == 5 -> return R.string.ferie_victoire2GM
            day == 14 && month == 7 -> return R.string.ferie_feteNationale
            day == 15 && month == 8 -> return R.string.ferie_assomption
            day == 1 && month == 11 -> return R.string.ferie_toussaint
            day == 11 && month == 11 -> return R.string.ferie_armistice
            day == 25 && month == 12 -> return R.string.ferie_noel
            isDroitLocal && day == 26 && month == 12 -> return R.string.ferie_saintEtienne
        }

        val easter = getEasterSunday(year)
        when (date) {
            easter.plusDays(1) -> return R.string.ferie_paque
            easter.plusDays(39) -> return R.string.ferie_ascension
            easter.plusDays(50) -> return R.string.ferie_pentecote
        }

        if (isDroitLocal && date == easter.minusDays(2)) {
            return R.string.ferie_vendrediSaint
        }

        return null
    }

    fun checkUpcomingHoliday(isDroitLocal: Boolean): HolidayAlertData? {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        getHolidayNameRes(today, isDroitLocal)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TODAY, holidayStringRes = resId)
        }

        getHolidayNameRes(tomorrow, isDroitLocal)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TOMORROW, holidayStringRes = resId)
        }

        return null
    }
}

enum class DayType { TODAY, TOMORROW }
data class HolidayAlertData(val targetDayType: DayType, val holidayStringRes: Int)