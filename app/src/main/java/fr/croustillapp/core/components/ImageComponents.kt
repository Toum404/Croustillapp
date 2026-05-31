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

/**
 * Chargeur d'images distant optimisé, enveloppant AsyncImage de Coil avec des solutions de repli unifiées.
 * Optimized remote image loader wrapper hosting Coil's AsyncImage with unified fallback behaviors.
 */
@Composable
fun AppImage(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    // Mise en cache des painters de couleur pour éliminer les allocations lors des recompositions de listes
    // Caching color painters to reduce object allocation penalties during layout updates
    val errorPainter = remember(primaryColor) { ColorPainter(primaryColor) }
    val placeholderPainter = remember(surfaceColor) { ColorPainter(surfaceColor) }

    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
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