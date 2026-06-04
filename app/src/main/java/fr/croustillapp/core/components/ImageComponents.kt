package fr.croustillapp.core.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

/**
 * FR: Composant personnalisé d'affichage d'images asynchrones utilisant la bibliothèque Coil.
 * EN: Custom asynchronous image loading component leveraging the Coil library.
 */

@Composable
fun AppImage(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    // FR: Optimisation de la mémoire en mémorisant les instances de ColorPainter pour éviter des allocations répétées.
    // EN: Memory optimization by remembering ColorPainter instances to avoid repeated allocations.
    val errorPainter = remember(primaryColor) { ColorPainter(primaryColor) }
    val placeholderPainter = remember(surfaceColor) { ColorPainter(surfaceColor) }

    // FR: Mémorisation de la requête d'image Coil; elle ne se re-déclenche que si l'URL change.
    // EN: Caching the Coil image request; it will only re-execute if the target URL updates.
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .dispatcher(kotlinx.coroutines.Dispatchers.IO)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier.background(surfaceColor),
        contentScale = ContentScale.Crop,
        placeholder = placeholderPainter,
        error = errorPainter,
        fallback = errorPainter
    )
}