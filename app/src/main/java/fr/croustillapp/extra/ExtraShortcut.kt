package fr.croustillapp.extra

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import fr.croustillapp.data.model.RestaurantEntity
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.visual.main.MainActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

// FR: Gestionnaire des raccourcis dynamiques de l'application
// EN: Manager for the app's dynamic shortcuts
object ShortcutHelper {
    private const val PRIMARY_COLOR = 0xFFEB334C.toInt()

    // FR: Met a jour les raccourcis dynamiques bases sur les favoris (en parallele pour plus de perf)
    // EN: Updates dynamic shortcuts based on favorites (in parallel for better performance)
    suspend fun updateFavoriteShortcuts(
        context: Context,
        favoriteRestaurants: List<RestaurantEntity>,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        val restaurantsToDisplay = favoriteRestaurants.take(AppDimens.FAVORIS_MAX)
        val imageLoader = context.imageLoader

        val shortcuts = coroutineScope {
            restaurantsToDisplay.map { restaurant ->
                async(ioDispatcher) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://croustillant.menu/r/${restaurant.id}".toUri()
                    ).apply {
                        setClass(context, MainActivity::class.java)
                    }

                    val iconCompat = resolveRestaurantIcon(context, imageLoader, restaurant.imageUrl, ioDispatcher)

                    ShortcutInfoCompat.Builder(context, "shortcut_restaurant_${restaurant.id}")
                        .setShortLabel(restaurant.name)
                        .setLongLabel("Voir ${restaurant.name}")
                        .setIcon(iconCompat)
                        .setIntent(intent)
                        .build()
                }
            }.awaitAll()
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    // FR: Resout et cree l'icone du restaurant (image ou couleur de secours)
    // EN: Resolves and creates the restaurant icon (image or fallback color)
    private suspend fun resolveRestaurantIcon(
        context: Context,
        imageLoader: ImageLoader,
        imageUrl: String?,
        ioDispatcher: CoroutineDispatcher
    ): IconCompat = withContext(ioDispatcher) {
        if (!imageUrl.isNullOrEmpty()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val originalBitmap = drawableToBitmap(drawable)
                    val fullBleedBitmap = fullBleedCircleBitmap(originalBitmap)

                    return@withContext IconCompat.createWithAdaptiveBitmap(fullBleedBitmap)
                }
            } catch (_: Exception) {
                return@withContext createColorBitmapIcon()
            }
        }

        return@withContext createColorBitmapIcon()
    }

    // FR: Convertit un Drawable en Bitmap
    // EN: Converts a Drawable to a Bitmap
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // FR: Cree un cercle parfait en plein ecran (Center Crop) avec la couleur de fond
    // EN: Creates a full-bleed perfect circle (Center Crop) with the background color
    private fun fullBleedCircleBitmap(source: Bitmap): Bitmap {
        val targetSize = 192
        val outputBitmap = createBitmap(targetSize, targetSize)
        val canvas = Canvas(outputBitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val radius = (targetSize / 2).toFloat()
        paint.color = PRIMARY_COLOR
        canvas.drawCircle(radius, radius, radius, paint)

        val path = Path().apply {
            addCircle(radius, radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(path)

        val srcWidth = source.width
        val srcHeight = source.height

        val scale: Float
        val dx: Float
        val dy: Float

        if (srcWidth * targetSize > targetSize * srcHeight) {
            scale = targetSize.toFloat() / srcHeight.toFloat()
            dx = (targetSize - srcWidth * scale) * 0.5f
            dy = 0f
        } else {
            scale = targetSize.toFloat() / srcWidth.toFloat()
            dx = 0f
            dy = (targetSize - srcHeight * scale) * 0.5f
        }

        val destRect = RectF(dx, dy, dx + srcWidth * scale, dy + srcHeight * scale)
        canvas.drawBitmap(source, null, destRect, paint)

        return outputBitmap
    }

    // FR: Cree une icone unie coloree de secours si aucune image n'est disponible
    // EN: Creates a solid fallback color icon if no image is available
    private fun createColorBitmapIcon(): IconCompat {
        val size = 192
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = PRIMARY_COLOR
            isAntiAlias = true
        }
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2).toFloat(), paint)

        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }
}