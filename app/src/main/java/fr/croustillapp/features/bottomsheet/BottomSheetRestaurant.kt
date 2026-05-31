package fr.croustillapp.features.bottomsheet

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.core.components.AppImage
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.features.elements.RestaurantViewModel
import fr.croustillapp.ui.theme.Jersey10Family
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

// Répétition du SVG personnalisé pour créer un fondu pixelisé.
// Repeated SVG to create a smooth pixel transition.
@Composable
fun PixelNoiseFade(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val vectorPainter = painterResource(id = R.drawable.pixel_fade_noise)

    Box(
        modifier = modifier.drawBehind {
            val colorFilter = ColorFilter.tint(color)

            // Calcul des dimensions exactes en pixels pour répéter le motif.
            // Calculate exact pixel dimensions for pattern repetition.
            val patternWidthPx = 100.dp.toPx().roundToInt()
            val patternHeightPx = size.height.roundToInt()
            val totalRepetitions = (size.width.roundToInt() / patternWidthPx) + 1

            for (i in 0 until totalRepetitions) {
                val xOffset = i * patternWidthPx

                drawContext.canvas.save()
                drawContext.transform.translate(left = xOffset.toFloat(), top = 0f)

                with(vectorPainter) {
                    draw(
                        size = Size((patternWidthPx + 1).toFloat(), patternHeightPx.toFloat()),
                        colorFilter = colorFilter
                    )
                }
                drawContext.canvas.restore()
            }
        }
    )
}

// Conteneur qui affiche les détails du restaurant, des actions rapides et les menus.
// Main sheet container displaying restaurant details, interactive quick actions, and menus.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantBottomSheet(
    restaurant: Restaurant,
    isFavorite: Boolean,
    viewModel: RestaurantViewModel = viewModel(),
    onFavoriteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val sheetColor = BottomSheetDefaults.ContainerColor

    val scrollState = rememberScrollState()

    // Mise en cache des chaînes pour éviter les requêtes répétées pendant les recompositions.
    // Caching string resources to prevent repeated lookups during recompositions.
    val toastAdresse = stringResource(R.string.toast_btn_adresse)
    val toastTel = stringResource(R.string.toast_btn_telephone)
    val toastEmail = stringResource(R.string.toast_btn_courriel)
    val toastPartage = stringResource(R.string.toast_btn_partager)
    val labelCopie = stringResource(R.string.copie_action)

    val btnCarte = stringResource(R.string.action_btn_carte)
    val btnTel = stringResource(R.string.action_btn_telephoner)
    val btnEmail = stringResource(R.string.action_btn_courriel)
    val btnPartage = stringResource(R.string.action_btn_partager)

    val shareUrl = remember(restaurant.id) { restaurant.generateShareUrl() }
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()

    // Déclenche la synchronisation des menus dès que le restaurant ciblé change.
    // Triggers full menu synchronization whenever target venue context shifts.
    LaunchedEffect(restaurant.id) {
        viewModel.loadMenu(restaurant.id)
    }

    // Réinitialisation de sécurité du scroll lorsque l'utilisateur déplie la feuille.
    // Auto-scroll safety reset when user manually expands sheet layout parameters.
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
            scrollState.scrollTo(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    AppImage(
                        url = restaurant.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(170.dp)
                    )

                    PixelNoiseFade(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.BottomCenter),
                        color = sheetColor
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 16.dp)) {

                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Jersey10Family,
                        fontSize = 36.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (restaurant.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                        if (restaurant.isOpen) {
                            val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")

                            val radarScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 2.5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 3500, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RadarScale"
                            )

                            val radarAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.35f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 3500, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RadarAlpha"
                            )

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .graphicsLayer(
                                            scaleX = radarScale,
                                            scaleY = radarScale,
                                            alpha = radarAlpha
                                        )
                                        .background(statusColor, CircleShape)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(6.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = stringResource(
                                id = R.string.statut_actuellement,
                                if (restaurant.isOpen) stringResource(R.string.statut_ouvert).lowercase(Locale.ROOT)
                                else stringResource(R.string.statut_ferme).lowercase(Locale.ROOT)
                            ),
                            color = statusColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val adresseOptions = remember(restaurant.adresse, toastAdresse, labelCopie, btnCarte, context, scope, clipboard) {
                            listOf(
                                labelCopie to {
                                    restaurant.adresse?.let { copyToClipboard(scope, clipboard, context, toastAdresse, it) }
                                    Unit
                                },
                                btnCarte to {
                                    try {
                                        val uri = "geo:${restaurant.latitude},${restaurant.longitude}?q=${Uri.encode(restaurant.adresse)}".toUri()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_carte, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_carte,
                            label = stringResource(R.string.label_btn_adresse),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.adresse.isNullOrBlank(),
                            options = adresseOptions
                        )

                        val telOptions = remember(restaurant.telephone, toastTel, labelCopie, btnTel, context, scope, clipboard) {
                            listOf(
                                labelCopie to {
                                    restaurant.telephone?.let { copyToClipboard(scope, clipboard, context, toastTel, it) }
                                    Unit
                                },
                                btnTel to {
                                    try {
                                        val cleanNumber = restaurant.telephone?.replace("[^0-9]".toRegex(), "")
                                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$cleanNumber".toUri()))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_telephone, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_telephone,
                            label = stringResource(R.string.label_btn_telephone),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.telephone.isNullOrBlank(),
                            options = telOptions
                        )

                        val emailOptions = remember(restaurant.email, toastEmail, labelCopie, btnEmail, context, scope, clipboard) {
                            listOf(
                                labelCopie to {
                                    restaurant.email?.let { copyToClipboard(scope, clipboard, context, toastEmail, it) }
                                    Unit
                                },
                                btnEmail to {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:${restaurant.email}".toUri())
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_courriel, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_courriel,
                            label = stringResource(R.string.label_btn_courriel),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.email.isNullOrBlank(),
                            options = emailOptions
                        )

                        val shareOptions = remember(shareUrl, toastPartage, labelCopie, btnPartage, context, scope, clipboard) {
                            listOf(
                                labelCopie to {
                                    copyToClipboard(scope, clipboard, context, toastPartage, shareUrl)
                                },
                                btnPartage to {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.toast_non_partage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_partager,
                            label = stringResource(R.string.label_btn_partager),
                            modifier = Modifier.weight(1f),
                            options = shareOptions
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = menuState) {
                        is RestaurantViewModel.MenuUiState.Loading -> MenuLoadingView()
                        is RestaurantViewModel.MenuUiState.Error -> MenuErrorView { viewModel.loadMenu(restaurant.id) }
                        is RestaurantViewModel.MenuUiState.Success -> {
                            MenuSection(
                                dailyMenus = state.data,
                                onMenuSelected = { /* ... */ }
                            )
                        }
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ScheduleSection(
                        horaires = restaurant.horaires,
                        isStrasbourg = restaurant.region.equals("Strasbourg", ignoreCase = true)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RestaurantFeaturesSection(restaurant = restaurant)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_fermer),
                contentDescription = stringResource(id = R.string.action_fermer),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                    .padding(8.dp)
            )

            Icon(
                painter = painterResource(id = if (isFavorite) R.drawable.ic_favori_oui else R.drawable.ic_favori_non),
                contentDescription = stringResource(id = R.string.action_favoriser),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onFavoriteClick() }
                    .padding(8.dp)
            )
        }
    }
}