package fr.croustillapp.features.bottomsheet

import android.content.ClipData
import android.content.Context
import android.content.Intent
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

/**
 * FR: Extension générant un slug URL standardisé et propre à partir du nom du restaurant pour le partage.
 * EN: Extension generating a clean, SEO-friendly URL slug from the restaurant name for external sharing.
 */
fun Restaurant.generateShareUrl(): String {
    val baseUrl = "https://croustillant.menu/fr/restaurants/"

    // FR: ① Décomposition des caractères accentués (ex: 'é' devient 'e' + accent flottant).
    // EN: ① Decouples accented characters (e.g., 'é' breaks down into 'e' + floating modifier).
    val cleanedName = name
        .lowercase()
        // FR: ② Nettoyage par Regex pour supprimer les résidus d'accents isolés.
        // EN: ② Regex cleaning pattern to strip away isolated residual modifier marks.
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // FR: ③ Remplacement des caractères spéciaux et espaces par des tirets uniques.
        // EN: ③ Swaps special characters and spaces out in favor of isolated single dashes.
        .replace("[^a-z0-9]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .trim('-')

    return "$baseUrl$cleanedName-r$id"
}

/**
 * FR: Formate une date brute de l'API et y injecte dynamiquement la chaîne localisée "Aujourd'hui" si applicable.
 * EN: Formats a raw API date string and dynamically appends the localized "Today" context token if applicable.
 */
fun formatApiDate(context: Context, dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        val outputFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE)

        val date = inputFormat.parse(dateStr) ?: return dateStr
        val formatted = outputFormat.format(date).replaceFirstChar { it.uppercase() }

        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        // FR: Comparaison stricte des métadonnées temporelles de l'année et du jour de l'année.
        // EN: Strict evaluation matching calendar parameters across year and day-of-year records.
        val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val todayStr = context.getString(R.string.ajd)

        if (isToday) "$formatted ($todayStr)" else formatted
    } catch (_: Exception) {
        dateStr
    }
}

/**
 * FR: Copie de manière asynchrone une chaîne de texte dans le Presse-papiers système Android.
 * EN: Asynchronously copies text payloads onto the Android OS system clipboard framework.
 */
fun copyToClipboard(
    scope: CoroutineScope,
    clipboard: Clipboard,
    context: Context,
    text: String
) {
    scope.launch {
        val clipData = ClipData.newPlainText("txt", text)

        clipboard.setClipEntry(ClipEntry(clipData))

        val message = context.getString(R.string.copie_toast)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}



/**
 * FR: Fonction helper (en attente d'intégration) pour générer un Intent de partage d'image de menu via l'API.
 * EN: Helper function (pending integration) building an outbound sharing Intent for menu images via API endpoints.
 */
@Suppress("UNUSED")
fun shareMenuImage(context: Context, restaurantId: String, date: String) {
    val imageUrl = "https://api.croustillant.menu/v1/restaurants/$restaurantId/menu/$date/image"

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Regarde le menu de ce restaurant ! \n\n$imageUrl")
        type = "text/plain"
    }

    try {
        val shareIntent = Intent.createChooser(sendIntent, "Partager le menu")
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "Impossible de partager le menu", Toast.LENGTH_SHORT).show()
    }
}