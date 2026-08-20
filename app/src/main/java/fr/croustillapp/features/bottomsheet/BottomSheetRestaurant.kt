package fr.croustillapp.features.bottomsheet

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.core.components.AppImage
import fr.croustillapp.features.data.DailyMenuDto
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.features.elements.RestaurantViewModel
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.widget.RestaurantWidgetReceiver
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * FR: Panneau d'affichage detaille du restaurant incluant la gestion d'etat UI et d'animations de statut.
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

    val isSheetStable = sheetState.currentValue == sheetState.targetValue
    val scrollState = rememberScrollState()

    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    val closeInteractionSource = remember { MutableInteractionSource() }
    val favoriteInteractionSource = remember { MutableInteractionSource() }

    val isClosePressed by closeInteractionSource.collectIsPressedAsState()
    val isFavoritePressed by favoriteInteractionSource.collectIsPressedAsState()

    val labelCopie = stringResource(R.string.copie_action)

    val btnCarte = stringResource(R.string.action_btn_carte)
    val btnTel = stringResource(R.string.action_btn_telephoner)
    val btnEmail = stringResource(R.string.action_btn_courriel)
    val btnPartage = stringResource(R.string.action_btn_partager)
    val btnBrowser = stringResource(R.string.action_btn_web)
    val btnWidget = stringResource(R.string.action_btn_widget)

    val errorWidget = stringResource(R.string.toast_non_widget)

    val shareUrl = remember(restaurant.id) { restaurant.generateShareUrl() }
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()

    val isExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()

    val isLocationEnabled by viewModel.isLocationEnabledOnDevice.collectAsStateWithLifecycle()

    val textMiniDistance = stringResource(R.string.mini_distance)
    val textLocationDisabled = stringResource(R.string.localisation_desactivee)

    val distanceMetersTemplate = stringResource(R.string.distance_meters)
    val distanceKilometersTemplate = stringResource(R.string.distance_kilometers)

    val filteredRestaurants by viewModel.filteredRestaurants.collectAsStateWithLifecycle()
    val currentRestaurant = remember(filteredRestaurants, restaurant.id) {
        filteredRestaurants.find { it.id == restaurant.id } ?: restaurant
    }

    // FR: Calcul et formatage de la distance selon le degre de precision de la geolocalisation accordee.
    // EN: Distance computing and layout formatting according to current geolocation accuracy level.
    val distanceLabel = remember(currentRestaurant.id, currentRestaurant.distance, isExact, isLocationEnabled, textMiniDistance, textLocationDisabled) {
        if (!isLocationEnabled) {
            return@remember " | $textLocationDisabled"
        }

        val dist = currentRestaurant.distance ?: return@remember ""

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

    // FR: Chargement asynchrone des donnees de menu lie a l'identifiant du currentRestaurant.
    // EN: Asynchronous loading execution pipeline linked to the active restaurant identifier.
    LaunchedEffect(currentRestaurant.id) {
        viewModel.loadMenu(currentRestaurant.id)
    }

    // FR: Reinitialisation automatique du defilement lorsque le composant change de taille ou d'etat d'ancrage.
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
                    .verticalScroll(scrollState, enabled = isSheetStable)
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
                        url = currentRestaurant.imageUrl,
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
                        text = currentRestaurant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Jersey10Family,
                        fontSize = 36.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (currentRestaurant.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                        if (currentRestaurant.isOpen) {
                            // FR: Animation infinie simulant l'effet visuel de pulsation d'un radar pour les etablissements ouverts.
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
                                if (currentRestaurant.isOpen) stringResource(R.string.statut_ouvert).lowercase(Locale.ROOT)
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
                        // FR: Configuration des Intents cartographiques avec gestion securisee des exceptions.
                        // EN: Map routing Intents generation alongside safety exception handling wrappers.
                        val adresseOptions = remember(currentRestaurant.id) {
                            listOf(
                                Triple(
                                    btnCarte,
                                    R.drawable.ic_ddm_map
                                )
                                {
                                    try {
                                        val labelResto = Uri.encode(currentRestaurant.name)
                                        val uri =
                                            "geo:${currentRestaurant.latitude},${currentRestaurant.longitude}?q=${currentRestaurant.latitude},${currentRestaurant.longitude}($labelResto)".toUri()
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
                                    currentRestaurant.adresse?.let {
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
                            enabled = !currentRestaurant.adresse.isNullOrBlank(),
                            options = adresseOptions
                        )

                        // FR: Configuration de l'Intent d'appel avec filtrage des caracteres non numeriques.
                        // EN: Dial Intents generation featuring non-numeric string formatting filters.
                        val telOptions = remember(currentRestaurant.id) {
                            listOf(
                                Triple(
                                    btnTel,
                                    R.drawable.ic_ddm_call
                                )
                                {
                                    try {
                                        val cleanNumber =
                                            currentRestaurant.telephone?.replace("[^0-9]".toRegex(), "")
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
                                    currentRestaurant.telephone?.let {
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
                            enabled = !currentRestaurant.telephone.isNullOrBlank(),
                            options = telOptions
                        )

                        // FR: Configuration de l'Intent de messagerie electronique (mailto:).
                        // EN: Mailing Intents initialization targeting specific endpoints (mailto:).
                        val emailOptions = remember(currentRestaurant.id) {
                            listOf(
                                Triple(
                                    btnEmail,
                                    R.drawable.ic_ddm_write
                                )
                                {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_SENDTO,
                                            "mailto:${currentRestaurant.email}".toUri()
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
                                    currentRestaurant.email?.let {
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
                            enabled = !currentRestaurant.email.isNullOrBlank(),
                            options = emailOptions
                        )

                        val actionsOptions = remember(currentRestaurant.id) {
                            listOf(
                                Triple(
                                    btnPartage,
                                    R.drawable.ic_ddm_link
                                ) {
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
                                    btnBrowser,
                                    R.drawable.ic_ddm_browse
                                ) {
                                    try {
                                        val webIntent = Intent.makeMainSelectorActivity(
                                            Intent.ACTION_MAIN,
                                            Intent.CATEGORY_APP_BROWSER
                                        ).apply {
                                            data = shareUrl.toUri()
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(webIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.toast_non_web,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                Triple(
                                    btnWidget,
                                    R.drawable.ic_ddm_widget
                                ) {
                                    val appWidgetManager = AppWidgetManager.getInstance(context)
                                    val myWidgetProvider = ComponentName(context, RestaurantWidgetReceiver::class.java)

                                    val sharedPrefs = context.getSharedPreferences("restaurant_widget_prefs", Context.MODE_PRIVATE)
                                    sharedPrefs.edit {
                                        putString(
                                            "last_selected_restaurant_id",
                                            currentRestaurant.id
                                        )
                                    }

                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(myWidgetProvider, null, null)
                                    } else {
                                        Toast.makeText(context, errorWidget, Toast.LENGTH_SHORT).show()
                                    }
                                    Unit
                                }
                            )
                        }
                        ActionIconItemWithMenu(
                            iconRes = R.drawable.ic_act_actions,
                            label = stringResource(R.string.label_btn_plus),
                            modifier = Modifier.weight(1f),
                            options = actionsOptions
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var selectedMenu by remember { mutableStateOf<DailyMenuDto?>(null) }

                    // FR: Gestion de l'affichage de l'etat du menu via une structure de contrôle scellee.
                    // EN: Dispatched layout routing based entirely on sealed menu UiState parameters.
                    when (val state = menuState) {
                        is RestaurantViewModel.MenuUiState.Loading -> MenuLoadingView()
                        is RestaurantViewModel.MenuUiState.Error -> MenuErrorView { viewModel.loadMenu(currentRestaurant.id) }
                        is RestaurantViewModel.MenuUiState.Success -> {
                            MenuSection(
                                restaurantId = currentRestaurant.id,
                                dailyMenus = state.data,
                                onMenuSelected = { menu ->
                                    selectedMenu = menu
                                }
                            )
                        }
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ScheduleSection(
                        horaires = currentRestaurant.horaires,
                        joursOuverts = currentRestaurant.joursOuvert,
                        isStrasbourg = currentRestaurant.region.equals("Strasbourg", ignoreCase = true)
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

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                AnimatedVisibility(
                    visible = isOffline,
                    enter = fadeIn() + scaleIn(initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(R.string.bts_cache),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 50.dp, vertical = 10.dp)
                        )
                    }
                }
            }

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