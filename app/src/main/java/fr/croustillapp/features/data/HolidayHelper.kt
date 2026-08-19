package fr.croustillapp.features.data

import fr.croustillapp.R
import java.util.Calendar

/**
 * FR: Classe utilitaire pour le calcul des jours feries fixes et mobiles (Algorithme de Meeus).
 * EN: Utility helper tracking fixed and moving public holidays (Meeus/Jones/Butcher algorithm).
 */
object HolidayHelper {

    /**
     * FR: Classe utilitaire pour le calcul des jours feries fixes et mobiles (Algorithme de Meeus).
     * EN: Utility helper tracking fixed and moving public holidays (Meeus/Jones/Butcher algorithm).
     */
    private fun getEasterSunday(year: Int): Calendar {
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

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * FR: Renvoie la ressource de chaine associee a un jour ferie, incluant les specificites d'Alsace-Moselle.
     * EN: Returns the string resource attached to a specific holiday, featuring localized Alsace-Moselle checks.
     */
    fun getHolidayNameRes(calendar: Calendar, isStrasbourg: Boolean): Int? {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        // FR: Jours feries fixes nationaux
        // EN: Static country-wide public holidays
        if (day == 1 && month == 1) return R.string.holiday_new_year
        if (day == 1 && month == 5) return R.string.holiday_labor_day
        if (day == 8 && month == 5) return R.string.holiday_victory_1945
        if (day == 14 && month == 7) return R.string.holiday_national_day
        if (day == 15 && month == 8) return R.string.holiday_assumption
        if (day == 1 && month == 11) return R.string.holiday_all_saints
        if (day == 11 && month == 11) return R.string.holiday_armistice_1918
        if (day == 25 && month == 12) return R.string.holiday_christmas

        // FR: Droit local (Saint-etienne)
        // EN: Local regional law (Boxing Day)
        if (isStrasbourg && day == 26 && month == 12) return R.string.holiday_boxing_day

        val easter = getEasterSunday(year)

        // FR: Jours feries mobiles bases sur Pâques (Pâques + 1, + 39, + 50)
        // EN: Dynamic shifting holidays computed relative to Easter Sunday offsets
        val easterMonday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (isSameDay(calendar, easterMonday)) return R.string.holiday_easter_monday

        val ascension = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 39) }
        if (isSameDay(calendar, ascension)) return R.string.holiday_ascension

        val whitMonday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 50) }
        if (isSameDay(calendar, whitMonday)) return R.string.holiday_whit_monday

        // FR: Droit local (Vendredi Saint : Pâques - 2)
        // EN: Local regional law (Good Friday)
        if (isStrasbourg) {
            val goodFriday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }
            if (isSameDay(calendar, goodFriday)) return R.string.holiday_good_friday
        }

        return null
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * FR: evalue la presence d'un jour ferie aujourd'hui ou demain pour declencher une alerte visuelle.
     * EN: Evaluates holiday triggers for today or tomorrow timelines to fire a proactive UI alert state.
     */
    fun checkUpcomingHoliday(isStrasbourg: Boolean): HolidayAlertData? {
        val current = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        getHolidayNameRes(current, isStrasbourg)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TODAY, holidayStringRes = resId)
        }

        current.add(Calendar.DAY_OF_YEAR, 1)
        getHolidayNameRes(current, isStrasbourg)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TOMORROW, holidayStringRes = resId)
        }

        return null
    }
}

enum class DayType { TODAY, TOMORROW }
data class HolidayAlertData(val targetDayType: DayType, val holidayStringRes: Int)