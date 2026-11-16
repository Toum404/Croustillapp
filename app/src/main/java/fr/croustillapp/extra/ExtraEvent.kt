package fr.croustillapp.extra

import android.content.Intent
import android.widget.Toast
import fr.croustillapp.R
import fr.croustillapp.data.model.UiMenu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun addMenuToCalendar(
    context: android.content.Context,
    restaurantName: String,
    restaurantAddress: String,
    currentMenu: UiMenu,
    errorToast: String
) {
    try {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateObj = sdf.parse(currentMenu.date) ?: Date()

        val calendar = Calendar.getInstance().apply {
            time = dateObj
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = startTime + (60 * 60 * 1000)

        val descriptionBuilder = StringBuilder()
        currentMenu.repas.forEach { repas ->
            repas.categories.forEach { cat ->
                descriptionBuilder.append("${cat.libelle.uppercase()}\n")
                cat.plats.forEach { plat ->
                    descriptionBuilder.append("• ${plat.libelle}\n")
                }
                descriptionBuilder.append("\n")
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.Events.TITLE, "Midi à $restaurantName")
            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, descriptionBuilder.toString().trim())
            putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, restaurantAddress)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.error_calendrier), Toast.LENGTH_SHORT).show()
            }
        }, 100)

    } catch (e: Exception) {
        e.printStackTrace()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, errorToast, Toast.LENGTH_SHORT).show()
        }
    }
}