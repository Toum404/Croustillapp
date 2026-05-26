package fr.croustillapp.components

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

@Composable
fun AppImage(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

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