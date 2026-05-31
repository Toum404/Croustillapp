package fr.croustillapp.features.data

import fr.croustillapp.R
import java.util.Calendar

/**
 * Utilitaire de calcul et d'analyse des jours fériés français avec prise en compte de l'Alsace.
 * Calendar compute provider checking country and regional holidays to match operational alerts.
 */
object HolidayHelper {

    /**
     * Calcule le dimanche de Pâques pour une année donnée via l'algorithme Meeus/Jones/Butcher.
     * Computes Easter Sunday matching an explicit calendar timeline loop via the Meeus/Jones/Butcher method.
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
     * Recherche la ressource de chaîne associée à un jour férié pour une date et une zone données.
     * Resolves matching string labels for legal dates, including custom local regional checks.
     *
     * @param isStrasbourg Activer pour inclure les règles d'Alsace-Moselle (Vendredi Saint, Saint-Étienne).
     * @return L'identifiant de la ressource String ou null s'il s'agit d'un jour ouvrable standard.
     */
    fun getHolidayNameRes(calendar: Calendar, isStrasbourg: Boolean): Int? {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        // 1. Jours fériés nationaux fixes / Universal statutory localized fixed days
        if (day == 1 && month == 1) return R.string.holiday_new_year
        if (day == 1 && month == 5) return R.string.holiday_labor_day
        if (day == 8 && month == 5) return R.string.holiday_victory_1945
        if (day == 14 && month == 7) return R.string.holiday_national_day
        if (day == 15 && month == 8) return R.string.holiday_assumption
        if (day == 1 && month == 11) return R.string.holiday_all_saints
        if (day == 11 && month == 11) return R.string.holiday_armistice_1914
        if (day == 25 && month == 12) return R.string.holiday_christmas

        // Concordance régionale d'Alsace-Moselle (Saint-Étienne) / Regional local rule
        if (isStrasbourg && day == 26 && month == 12) return R.string.holiday_boxing_day

        // 2. Jours fériés mobiles basés sur Pâques / Dynamic legal holidays offset calculations
        val easter = getEasterSunday(year)

        // Lundi de Pâques (+1 jour) / Easter Monday
        val easterMonday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (isSameDay(calendar, easterMonday)) return R.string.holiday_easter_monday

        // Ascension (+39 jours) / Ascension Thursday
        val ascension = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 39) }
        if (isSameDay(calendar, ascension)) return R.string.holiday_ascension

        // Lundi de Pentecôte (+50 jours) / Whit Monday
        val whitMonday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 50) }
        if (isSameDay(calendar, whitMonday)) return R.string.holiday_whit_monday

        // Vendredi Saint d'Alsace (-2 jours avant Pâques) / Good Friday exception rule
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
     * Analyse l'existence d'une fête légale aujourd'hui ou demain pour déclencher un bandeau d'alerte.
     * Evaluates active date configurations against today or tomorrow vectors to feed info banners.
     */
    fun checkUpcomingHoliday(isStrasbourg: Boolean): HolidayAlertData? {
        val current = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Évaluation pour "Aujourd'hui" / Check for "Today"
        getHolidayNameRes(current, isStrasbourg)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TODAY, holidayStringRes = resId)
        }

        // Évaluation pour "Demain" / Check for "Tomorrow"
        current.add(Calendar.DAY_OF_YEAR, 1)
        getHolidayNameRes(current, isStrasbourg)?.let { resId ->
            return HolidayAlertData(targetDayType = DayType.TOMORROW, holidayStringRes = resId)
        }

        return null
    }
}

enum class DayType { TODAY, TOMORROW }
data class HolidayAlertData(val targetDayType: DayType, val holidayStringRes: Int)