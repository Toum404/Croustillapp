package fr.croustillapp.visual.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

@Composable
fun MainContainer(
    modifier: Modifier = Modifier,
    windowInsetsSides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
    isBottomBarVisible: Boolean = true,
    isMinimalModeEnabled: Boolean = false,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isBottomBarShown = !isLandscape && !isMinimalModeEnabled && isBottomBarVisible

    val dynamicBottomPadding = if (isBottomBarShown) AppDimens.barGraisseVertical else 0.dp

    val sidesToApply = if (isLandscape || !isBottomBarShown) {
        windowInsetsSides + WindowInsetsSides.Bottom
    } else {
        windowInsetsSides
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(sidesToApply)
            )
            .padding(horizontal = 16.dp)
            .padding(bottom = dynamicBottomPadding)
            .padding(top = if (!isLandscape) 4.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

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

fun shareText(context: Context, textToShare: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textToShare)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    } catch (_: Exception) {
        Toast.makeText(context, R.string.dialog_non_partage, Toast.LENGTH_SHORT).show()
    }
}

fun Restaurant.generateShareUrl(userLinkPreference: String = "Auto"): String {
    val localizedLang = if (userLinkPreference == "Auto") {
        val systemLocale = try {
            AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
        } catch (_: Exception) {
            Locale.getDefault()
        }
        if (systemLocale.language.startsWith("fr")) "fr" else "en"
    } else {
        if (userLinkPreference == "Français") "fr" else "en"
    }

    val baseUrl = "https://croustillant.menu/$localizedLang/restaurants/"

    // FR: ① Decomposition des caractères accentues.
    // EN: ① Decouples accented characters.
    val cleanedName = name
        .lowercase()
        // FR: ② Nettoyage par Regex pour supprimer les residus d'accents.
        // EN: ② Regex cleaning pattern to strip away isolated residual marks.
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // FR: ③ Remplacement des caractères speciaux et espaces par des tirets.
        // EN: ③ Swaps special characters and spaces out for dashes.
        .replace("[^a-z0-9]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .trim('-')

    return "$baseUrl$cleanedName-r$id"
}