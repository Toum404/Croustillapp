package fr.croustillapp.features.bottomsheet

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import fr.croustillapp.R
import fr.croustillapp.features.data.Restaurant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun isToday(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        val targetDate = sdf.parse(dateStr) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        targetDate == today
    } catch (_: Exception) {
        false
    }
}

fun isTomorrow(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        val targetDate = sdf.parse(dateStr) ?: return false
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        targetDate == tomorrow
    } catch (_: Exception) {
        false
    }
}

/**
 * FR: Extension generant un slug URL standardise et propre a partir du nom du restaurant pour le partage.
 * EN: Extension generating a clean, SEO-friendly URL slug from the restaurant name for external sharing.
 */
fun Restaurant.generateShareUrl(): String {
    val baseUrl = "https://croustillant.menu/fr/restaurants/"

    // FR: ① Decomposition des caracteres accentues (ex: 'é' devient 'e' + accent flottant).
    // EN: ① Decouples accented characters (e.g., 'é' breaks down into 'e' + floating modifier).
    val cleanedName = name
        .lowercase()
        // FR: ② Nettoyage par Regex pour supprimer les residus d'accents isoles.
        // EN: ② Regex cleaning pattern to strip away isolated residual modifier marks.
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // FR: ③ Remplacement des caracteres speciaux et espaces par des tirets uniques.
        // EN: ③ Swaps special characters and spaces out in favor of isolated single dashes.
        .replace("[^a-z0-9]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .trim('-')

    return "$baseUrl$cleanedName-r$id"
}

/**
 * FR: Copie de maniere asynchrone une chaine de texte dans le Presse-papiers systeme Android.
 * EN: Asynchronously copies text payloads onto the Android OS system clipboard framework.
 */
fun copyToClipboard(
    scope: CoroutineScope,
    clipboard: Clipboard,
    context: Context,
    text: String
) {
    scope.launch {
        try {
            val clipData = ClipData.newPlainText("txt", text)
            clipboard.setClipEntry(ClipEntry(clipData))

            val message = context.getString(R.string.copie_toast)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            val error = context.getString(R.string.menu_act_error)
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
}