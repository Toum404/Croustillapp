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
 * Génère une URL de partage propre et optimisée pour le SEO à partir du nom du restaurant.
 * Generates a clean, SEO-optimized sharing URL based on the restaurant's name.
 *
 * @return -> https://croustillant.menu/fr/restaurants/nom-du-resto-rID
 */
fun Restaurant.generateShareUrl(): String {
    val baseUrl = "https://croustillant.menu/fr/restaurants/"

    // 1. Passage en minuscules / Convert to lowercase
    val cleanedName = name
        .lowercase()
        // 2. Suppression des caractères accentués / Remove diacritical marks (accents)
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // 3. Remplacement de tout ce qui n'est pas alphanumérique par un tiret / Replace non-alphanumeric with dashes
        .replace("[^a-z0-9]".toRegex(), "-")
        // 4. Évite les doubles tirets successifs (ex: "l'eau" -> "l--eau" -> "l-eau") / Prevent duplicate consecutive dashes
        .replace("-+".toRegex(), "-")
        // 5. Nettoyage des tirets aux extrémités / Trim leading or trailing dashes
        .trim('-')

    // 6. Assemblage final : base + nom-nettoyé + -r + id / Final compilation
    return "$baseUrl$cleanedName-r$id"
}

/**
 * Formate une chaîne de date brute de l'API en une chaîne textuelle élégante et localisée.
 * Formats a raw API date string into a localized, user-friendly text presentation.
 *
 * @param dateStr La chaîne de date d'entrée (ex: "30-05-2026").
 * @return La date formatée (ex: "Samedi 30 mai 2026"), avec la mention "(Aujourd'hui)" si applicable.
 */
fun formatApiDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        val outputFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE)

        val date = inputFormat.parse(dateStr) ?: return dateStr
        val formatted = outputFormat.format(date).replaceFirstChar { it.uppercase() }

        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        // Vérification si la date correspond au jour actuel / Check if target calendar instance matches today
        val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isToday) "$formatted (Aujourd'hui)" else formatted
    } catch (_: Exception) {
        dateStr
    }
}

// Copie de manière asynchrone un texte brut dans le presse-papier du système Compose.
// Asynchronously copies a plain text string into the Compose framework clipboard layer.
fun copyToClipboard(
    scope: CoroutineScope,
    clipboard: Clipboard,
    context: Context,
    label: String,
    text: String
) {
    scope.launch {
        val clipData = ClipData.newPlainText(label, text)
        // Utilisation de l'API ClipEntry moderne de Compose / Leveraging modern Compose ClipEntry API wrappers
        clipboard.setClipEntry(ClipEntry(clipData))

        val message = "$label ${context.getString(R.string.copie_toast)}"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Lance une intention système pour partager l'URL de l'image du menu générée par l'API.
 * Dispatches a system share intent linking directly to the API-generated menu image resource.
 *
 * !!! Pas encore connectée aux composants UI graphiques / Not attached to any active graphic UI components for the moment.
 */
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
        // Sécurité contre l'absence d'application de partage disponible / Safety fallback for environments without share handlers
        Toast.makeText(context, "Impossible de partager le menu", Toast.LENGTH_SHORT).show()
    }
}