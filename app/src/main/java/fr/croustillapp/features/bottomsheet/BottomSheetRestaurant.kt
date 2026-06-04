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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
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

/**
 * FR: Effet de transition pixélisé personnalisé dessiné via l'API Canvas pour l'effet graphique inférieur de l'image.
 * EN: Custom pixelated fade effect drawn via low-level Canvas API for the bottom image graphic transition.
 */
@Composable
fun PixelNoiseFade(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val vectorPainter = painterResource(id = R.drawable.svg_pixel_noise)

    Box(
        modifier = modifier.drawBehind {
            val colorFilter = ColorFilter.tint(color)

            // FR: Calcul de la grille de répétition en fonction de la largeur dynamique de la vue.
            // EN: Compute pattern repetition based on the dynamic runtime view width.
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

/**
 * FR: Panneau d'affichage détaillé du restaurant incluant la gestion d'état UI et d'animations de statut.
 * EN: Detailed bottom sheet panel for a restaurant, including UI state handling and status animations.
 */
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

    val closeInteractionSource = remember { MutableInteractionSource() }
    val favoriteInteractionSource = remember { MutableInteractionSource() }

    val isClosePressed by closeInteractionSource.collectIsPressedAsState()
    val isFavoritePressed by favoriteInteractionSource.collectIsPressedAsState()

    val labelCopie = stringResource(R.string.copie_action)

    val btnCarte = stringResource(R.string.action_btn_carte)
    val btnTel = stringResource(R.string.action_btn_telephoner)
    val btnEmail = stringResource(R.string.action_btn_courriel)
    val btnPartage = stringResource(R.string.action_btn_partager)

    val shareUrl = remember(restaurant.id) { restaurant.generateShareUrl() }
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()

    val isExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()

    val textMiniDistance = stringResource(R.string.mini_distance)

    val distanceMetersTemplate = stringResource(R.string.distance_meters)
    val distanceKilometersTemplate = stringResource(R.string.distance_kilometers)

    // FR: Calcul et formatage de la distance selon le degré de précision de la géolocalisation accordée.
    // EN: Distance computing and layout formatting according to current geolocation accuracy level.
    val distanceLabel = remember(restaurant.id, restaurant.distance, isExact, textMiniDistance, distanceMetersTemplate, distanceKilometersTemplate) {
        val dist = restaurant.distance ?: return@remember ""

        // FR: Fonction utilitaire pure utilisant le formatage natif de chaînes Kotlin sans capture de contexte.
        // EN: Pure utility helper function utilizing native Kotlin string formatting without context capture.
        fun formatDistance(meters: Float): String {
            return if (meters < 1000f) {
                " | " + String.format(Locale.getDefault(), distanceMetersTemplate, meters.roundToInt())
            } else {
                " | " + String.format(Locale.getDefault(), distanceKilometersTemplate, meters / 1000f)
            }
        }

        if (!isExact) {
            if (dist < 5000f) {
                " | $textMiniDistance"
            } else {
                formatDistance(dist)
            }
        } else {
            formatDistance(dist)
        }
    }

    // FR: Chargement asynchrone des données de menu lié à l'identifiant du restaurant.
    // EN: Asynchronous loading execution pipeline linked to the active restaurant identifier.
    LaunchedEffect(restaurant.id) {
        viewModel.loadMenu(restaurant.id)
    }

    // FR: Réinitialisation automatique du défilement lorsque le composant change de taille ou d'état d'ancrage.
    // EN: Automatic scroll state reset when the overlay component switches layout anchoring targets.
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
                            // FR: Animation infinie simulant l'effet visuel de pulsation d'un radar pour les établissements ouverts.
                            // EN: Infinite animation setup mimicking a live sonar radar wave effect for open venues.
                            val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")

                            val radarScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 3.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RadarScale"
                            )
                            val radarAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.33f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RadarAlpha"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(x = (-6).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
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
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(x = (-6).dp)
                            ){
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                            }
                        }

                        Text(
                            text = stringResource(
                                id = R.string.statut_actuellement,
                                if (restaurant.isOpen) stringResource(R.string.statut_ouvert).lowercase(Locale.ROOT)
                                else stringResource(R.string.statut_ferme).lowercase(Locale.ROOT)
                            ),
                            color = statusColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.offset(x = (-6).dp)
                        )

                        if (distanceLabel.isNotEmpty()) {
                            Text(
                                text = distanceLabel,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.offset(x = (-6).dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // FR: Configuration des Intents cartographiques avec gestion sécurisée des exceptions.
                        // EN: Map routing Intents generation alongside safety exception handling wrappers.
                        val adresseOptions = remember(restaurant.id) {
                            listOf(
                                Triple(
                                    btnCarte,
                                    R.drawable.ic_ddm_map
                                )
                                {
                                    try {
                                        val labelResto = Uri.encode(restaurant.name)
                                        val uri =
                                            "geo:${restaurant.latitude},${restaurant.longitude}?q=${restaurant.latitude},${restaurant.longitude}($labelResto)".toUri()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.toast_non_carte,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                Triple(
                                    labelCopie,
                                    R.drawable.ic_ddm_copy
                                )
                                {
                                    restaurant.adresse?.let {
                                        copyToClipboard(
                                            scope,
                                            clipboard,
                                            context,
                                            it
                                        )
                                    }
                                    Unit
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_act_adress,
                            label = stringResource(R.string.label_btn_adresse),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.adresse.isNullOrBlank(),
                            options = adresseOptions
                        )

                        // FR: Configuration de l'Intent d'appel avec filtrage des caractères non numériques.
                        // EN: Dial Intents generation featuring non-numeric string formatting filters.
                        val telOptions = remember(restaurant.id) {
                            listOf(
                                Triple(
                                    btnTel,
                                    R.drawable.ic_ddm_call
                                )
                                {
                                    try {
                                        val cleanNumber =
                                            restaurant.telephone?.replace("[^0-9]".toRegex(), "")
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_DIAL,
                                                "tel:$cleanNumber".toUri()
                                            )
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.toast_non_telephone,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                Triple(
                                    labelCopie,
                                    R.drawable.ic_ddm_copy
                                )
                                {
                                    restaurant.telephone?.let {
                                        copyToClipboard(
                                            scope,
                                            clipboard,
                                            context,
                                            it
                                        )
                                    }
                                    Unit
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_act_phone,
                            label = stringResource(R.string.label_btn_telephone),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.telephone.isNullOrBlank(),
                            options = telOptions
                        )

                        // FR: Configuration de l'Intent de messagerie électronique (mailto:).
                        // EN: Mailing Intents initialization targeting specific endpoints (mailto:).
                        val emailOptions = remember(restaurant.id) {
                            listOf(
                                Triple(
                                    btnEmail,
                                    R.drawable.ic_ddm_write
                                )
                                {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_SENDTO,
                                            "mailto:${restaurant.email}".toUri()
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.toast_non_courriel,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                Triple(
                                    labelCopie,
                                    R.drawable.ic_ddm_copy
                                )
                                {
                                    restaurant.email?.let {
                                        copyToClipboard(
                                            scope,
                                            clipboard,
                                            context,
                                            it
                                        )
                                    }
                                    Unit
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_act_email,
                            label = stringResource(R.string.label_btn_courriel),
                            modifier = Modifier.weight(1f),
                            enabled = !restaurant.email.isNullOrBlank(),
                            options = emailOptions
                        )

                        // FR: Configuration du sélecteur natif d'applications de partage de texte.
                        // EN: Intent chooser initialization supplying local URLs across system sharing pipelines.
                        val shareOptions = remember(restaurant.id) {
                            listOf(
                                Triple(
                                    btnPartage,
                                    R.drawable.ic_ddm_link
                                )
                                {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                sendIntent,
                                                null
                                            )
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.toast_non_partage,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                Triple(
                                    labelCopie,
                                    R.drawable.ic_ddm_copy
                                )
                                {
                                    copyToClipboard(
                                        scope,
                                        clipboard,
                                        context,
                                        shareUrl
                                    )
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_act_share,
                            label = stringResource(R.string.label_btn_partager),
                            modifier = Modifier.weight(1f),
                            options = shareOptions
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FR: Gestion de l'affichage de l'état du menu via une structure de contrôle scellée.
                    // EN: Dispatched layout routing based entirely on sealed menu UiState parameters.
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
                        joursOuverts = restaurant.joursOuvert,
                        isStrasbourg = restaurant.region.equals("Strasbourg", ignoreCase = true)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RestaurantFeaturesSection(restaurant = restaurant)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_bs_close),
                contentDescription = stringResource(id = R.string.action_fermer),
                tint = if (isClosePressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = closeInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current
                    ) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                    .padding(8.dp)
            )

            Icon(
                painter = painterResource(
                    id = if (isFavorite) R.drawable.ic_bs_heart_filled else R.drawable.ic_bs_heart_outlined
                ),
                contentDescription = stringResource(id = R.string.action_favoriser),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .graphicsLayer(
                        scaleX = if (isFavoritePressed) 0.85f else 1.0f,
                        scaleY = if (isFavoritePressed) 0.85f else 1.0f,
                        alpha = if (isFavoritePressed) 0.7f else 1.0f
                    )
                    .clickable(
                        interactionSource = favoriteInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current
                    ) {
                        onFavoriteClick()
                    }
                    .padding(8.dp)
            )
        }
    }
}