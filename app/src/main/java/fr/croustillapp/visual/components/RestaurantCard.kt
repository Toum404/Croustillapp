package fr.croustillapp.visual.components

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import java.util.Locale
import kotlin.math.roundToInt

/**
 * FR: Composant carte principal affichant l'aperçu d'un restaurant (Image, Titre, Distance, Statut).
 * EN: Primary card layout showcasing a restaurant snapshot (Image, Title, Distance, Status Badge).
 */
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    isNew: Boolean,
    isPrecisionExact: Boolean,
    onClick: (Restaurant) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(AppDimens.cornerSmall) }

    val context = LocalContext.current

    val subtitleText = remember(context, restaurant.distance, restaurant.zone, isPrecisionExact) {
        formatDistance(
            context = context,
            distanceInMeters = restaurant.distance,
            defaultZone = restaurant.zone,
            isPrecisionExact = isPrecisionExact
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onClick(restaurant) },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Box {
                AppImage(
                    url = restaurant.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                if (isNew) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(0.dp)
                            .align(Alignment.TopStart)
                            .shadow(2.dp, RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                    ) {
                        Text(
                            text = stringResource(R.string.card_statut_new),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isFavorite) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bs_heart_on),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppDimens.paddingMedium)
                            .size(28.dp)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                            .padding(6.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(7.5.dp)) {
                Text(
                    text = restaurant.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Jersey10Family,
                    fontSize = 24.sp,
                    // FR: Force une hauteur constante de 2 lignes pour preserver l'alignement de la grille.
                    // EN: Forces a stable 2-line height constraint to keep consistent grid row alignment.
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(AppDimens.spacerBig))

                    StatusChip(isOpen = restaurant.isOpen)
                }
            }
        }
    }
}

/**
 * FR: Formate intelligemment la distance metrique en paliers ou bascule sur la zone globale de l'API.
 * EN: Intelligently formats metric distances into increments or falls back to generic API zone values.
 */
private fun formatDistance(context: Context, distanceInMeters: Float?, defaultZone: String, isPrecisionExact: Boolean): String {
    if (distanceInMeters == null) return defaultZone

    if (distanceInMeters >= AppDimens.DISTANCE_MAX) {
        return defaultZone
    }

    return if (isPrecisionExact) {
        if (distanceInMeters < 1000f) {
            // FR: Arrondi propre par tranche de 10 metres pour l'affichage de proximite immediate.
            // EN: Clean grouping rounded down to 10-meter boundaries for immediate proximity displays.
            val roundedMeters = (distanceInMeters / 10f).roundToInt() * 10
            "$roundedMeters m"
        } else {
            val kilometers = distanceInMeters / 1000f
            String.format(Locale.US, "%.1f km", kilometers)
        }
    } else {
        context.getString(R.string.restaurant_statut_proche)
    }
}

/**
 * FR: Badge compact de statut stylise indiquant si l'etablissement est ouvert ou ferme.
 * EN: Mini stylized status indicator pill stating if the facility is currently open or closed.
 */
@Composable
fun StatusChip(isOpen: Boolean) {
    val statusText = if (isOpen) stringResource(id = R.string.bs_statut_on) else stringResource(id = R.string.bs_statut_off)

    val backgroundColor = if (isOpen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isOpen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isOpen) Color.Transparent else MaterialTheme.colorScheme.outlineVariant

    Text(
        text = statusText,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .width(60.dp)
            .background(backgroundColor, RoundedCornerShape(AppDimens.cornerSmall))
            .border(1.dp, borderColor, RoundedCornerShape(AppDimens.cornerSmall))
            .padding(vertical = 4.dp),
        color = contentColor
    )
}

/**
 * FR: Squelette d'attente (Skeleton) repliquant l'anatomie de la carte avec une animation Shimmer.
 * EN: Placeholder skeleton replicating the exact card blueprint tied to a fluid Shimmer animation.
 */
@Composable
fun RestaurantCardSkeleton(brush: Brush) {
    val cardShape = RoundedCornerShape(AppDimens.cornerSmall)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .shimmer(brush)
            )

            Column(modifier = Modifier.padding(7.5.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer(brush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer(brush)
                )

                Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmer(brush)
                    )

                    Spacer(modifier = Modifier.width(AppDimens.spacerBig))

                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 20.dp)
                            .clip(RoundedCornerShape(AppDimens.cornerSmall))
                            .shimmer(brush)
                    )
                }
            }
        }
    }
}

/**
 * FR: Initialise et anime un Brush de gradient lineaire infini simulant un effet Shimmer scintillant.
 * EN: Remembers and drives an infinite linear gradient Brush simulating a glossy sweep shimmer effect.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "master_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_move"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

/**
 * FR: Modificateur d'extension appliquant le pinceau de scintillement sur l'arriere-plan du composant.
 * EN: Extension Modifier node piping the shimmer sweep brush across the element's background surface.
 */
fun Modifier.shimmer(brush: Brush): Modifier = this.background(brush)