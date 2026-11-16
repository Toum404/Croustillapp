package fr.croustillapp.visual.restaurant

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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.data.model.UiMenu
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.AppImage
import fr.croustillapp.visual.components.generateShareUrl
import fr.croustillapp.visual.components.shareText
import fr.croustillapp.visual.dialog.DialogContact
import fr.croustillapp.visual.dialog.DialogICS
import fr.croustillapp.visual.dialog.DialogWidget
import fr.croustillapp.visual.restaurant.components.BottomSheetButtonClose
import fr.croustillapp.visual.restaurant.components.BottomSheetHeaderContainer
import fr.croustillapp.visual.restaurant.components.BsSectionAction
import fr.croustillapp.visual.restaurant.components.BsSectionFeature
import fr.croustillapp.visual.restaurant.components.BsSectionMenu
import fr.croustillapp.visual.restaurant.components.BsSectionSchedule
import fr.croustillapp.visual.restaurant.components.MenuErrorView
import fr.croustillapp.visual.restaurant.components.MenuLoadingView
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
    val sheetColor = MaterialTheme.colorScheme.background

    val isSheetStable = sheetState.currentValue == sheetState.targetValue

    val listState = rememberLazyListState()

    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()

    LaunchedEffect(isOffline, menuState) {
        if (!isOffline && menuState is RestaurantViewModel.MenuUiState.Error) {
            viewModel.loadMenu(restaurant.id)
        }
    }

    val favoriteInteractionSource = remember { MutableInteractionSource() }

    val isFavoritePressed by favoriteInteractionSource.collectIsPressedAsState()

    var showExtraActions by remember { mutableStateOf(false) }

    var showAddressDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    val showWidgetDialog = remember { mutableStateOf(false) }
    val showIcsDialog = remember { mutableStateOf(false) }

    val btnCarte = stringResource(R.string.dialog_btn_carte)
    val btnTel = stringResource(R.string.dialog_btn_telephone)
    val btnEmail = stringResource(R.string.dialog_btn_courriel)
    val btnPartage = stringResource(R.string.dialog_btn_partager)
    val btnBrowser = stringResource(R.string.dialog_btn_web)
    val btnIcs = stringResource(R.string.dialog_btn_ics)
    val btnWidget = stringResource(R.string.dialog_btn_widget)

    val linkLanguagePref by viewModel.linkLanguage.collectAsStateWithLifecycle()
    val shareUrl = remember(restaurant.id, linkLanguagePref) {
        restaurant.generateShareUrl(linkLanguagePref)
    }

    val density = LocalDensity.current
    val headerHeightPx = with(density) { AppDimens.bsSizeHeader.toPx() }

    val isExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()

    val isLocationEnabled by viewModel.isLocationEnabledOnDevice.collectAsStateWithLifecycle()

    val textMiniDistance = stringResource(R.string.restaurant_statut_proche)
    val textLocationDisabled = stringResource(R.string.restaurant_statut_inconnu)

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
                "  |  " + String.format(Locale.getDefault(), distanceMetersTemplate, meters.roundToInt())
            } else {
                "  |  " + String.format(Locale.getDefault(), distanceKilometersTemplate, meters / 1000f)
            }
        }

        if (!isExact) {
            if (dist < AppDimens.DISTANCE_MAX) {
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
            listState.scrollToItem(0)

            if (showExtraActions) {
                showExtraActions = false
            }
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

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                userScrollEnabled = isSheetStable
            ) {
                item {
                    BottomSheetHeaderContainer(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        sheetColor = sheetColor
                    ) {
                        AppImage(
                            url = currentRestaurant.imageUrl,
                            modifier = Modifier.fillMaxWidth().height(AppDimens.bsSizeHeader)
                        )
                    }
                }

                item {
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
                            val statusColor = if (currentRestaurant.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            if (currentRestaurant.isOpen) {
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
                                    id = R.string.bs_statut_tit,
                                    if (currentRestaurant.isOpen) stringResource(R.string.bs_statut_on).lowercase(Locale.ROOT)
                                    else stringResource(R.string.bs_statut_off).lowercase(Locale.ROOT)
                                ),
                                color = statusColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.offset(x = (-6).dp)
                            )

                            if (distanceLabel.isNotEmpty()) {
                                Text(
                                    text = distanceLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.offset(x = (-6).dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                        BsSectionAction(
                            hasAddress = !currentRestaurant.adresse.isNullOrBlank(),
                            hasPhone = !currentRestaurant.telephone.isNullOrBlank(),
                            hasEmail = !currentRestaurant.email.isNullOrBlank(),
                            showExtraActions = showExtraActions,
                            labelAddress = stringResource(R.string.dialog_tit_adresse),
                            labelPhone = stringResource(R.string.dialog_tit_telephone),
                            labelEmail = stringResource(R.string.dialog_tit_courriel),
                            labelTogglePlus = stringResource(R.string.dialog_menu_plus),
                            labelToggleMoins = stringResource(R.string.dialog_menu_moins),
                            labelBrowser = btnBrowser,
                            labelWidget = btnWidget,
                            labelIcs = btnIcs,
                            labelShare = btnPartage,
                            onAddressClick = { showAddressDialog = true },
                            onPhoneClick = { showPhoneDialog = true },
                            onEmailClick = { showEmailDialog = true },
                            onToggleClick = {
                                if (!showExtraActions) {
                                    showExtraActions = true
                                    if (sheetState.currentValue != SheetValue.Expanded) {
                                        scope.launch { sheetState.expand() }
                                    }
                                } else {
                                    showExtraActions = false
                                }
                            },
                            onBrowserClick = {
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
                                    Toast.makeText(context, R.string.dialog_non_web, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onWidgetClick = { showWidgetDialog.value = true },
                            onIcsClick = { showIcsDialog.value = true },
                            onShareClick = { shareText(context, shareUrl) }
                        )

                        if (showAddressDialog) {
                            val rawAddress = currentRestaurant.adresse ?: ""
                            val uri = "geo:${currentRestaurant.latitude},${currentRestaurant.longitude}?q=${Uri.encode(currentRestaurant.name)}".toUri()
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            val canOpenMap = mapIntent.resolveActivity(context.packageManager) != null

                            DialogContact(
                                onDismissRequest = { showAddressDialog = false },
                                title = stringResource(R.string.dialog_tit_adresse),
                                contact = rawAddress,
                                error = stringResource(R.string.dialog_non_carte),
                                button1Text = btnCarte,
                                isButton1Enabled = canOpenMap,
                                onButton1Click = {
                                    showAddressDialog = false
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.dialog_non_carte, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (showPhoneDialog) {
                            val rawPhone = currentRestaurant.telephone ?: ""
                            val cleanNumber = rawPhone.replace(Regex("[^0-9+]"), "")
                            val phoneUri = "tel:$cleanNumber".toUri()
                            val phoneIntent = Intent(Intent.ACTION_DIAL, phoneUri)
                            val canMakePhoneCall = phoneIntent.resolveActivity(context.packageManager) != null

                            DialogContact(
                                onDismissRequest = { showPhoneDialog = false },
                                title = stringResource(R.string.dialog_tit_telephone),
                                contact = rawPhone,
                                error = stringResource(R.string.dialog_non_telephone),
                                button1Text = btnTel,
                                isButton1Enabled = canMakePhoneCall,
                                onButton1Click = {
                                    showPhoneDialog = false
                                    try {
                                        context.startActivity(phoneIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.dialog_non_telephone, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (showEmailDialog) {
                            val rawEmail = currentRestaurant.email ?: ""
                            val emailUri = "mailto:$rawEmail".toUri()
                            val emailIntent = Intent(Intent.ACTION_SENDTO, emailUri)
                            val canSendEmail = emailIntent.resolveActivity(context.packageManager) != null

                            DialogContact(
                                onDismissRequest = { showEmailDialog = false },
                                title = stringResource(R.string.dialog_tit_courriel),
                                contact = rawEmail,
                                error = stringResource(R.string.dialog_non_courriel),
                                button1Text = btnEmail,
                                isButton1Enabled = canSendEmail,
                                onButton1Click = {
                                    showEmailDialog = false
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, R.string.dialog_non_courriel, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (showWidgetDialog.value) {
                            DialogWidget(
                                onDismissRequest = { showWidgetDialog.value = false },
                                restaurantId = currentRestaurant.id
                            )
                        }

                        if (showIcsDialog.value) {
                            DialogICS(
                                title = btnIcs,
                                onDismissRequest = { showIcsDialog.value = false },
                                restaurant = currentRestaurant
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                        BsSectionSchedule(
                            horaires = currentRestaurant.horaires,
                            joursOuverts = currentRestaurant.joursOuvert,
                            zone = currentRestaurant.zone
                        )

                        Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                        var selectedMenu by remember { mutableStateOf<UiMenu?>(null) }

                        when (val state = menuState) {
                            is RestaurantViewModel.MenuUiState.Loading -> MenuLoadingView()
                            is RestaurantViewModel.MenuUiState.Error -> MenuErrorView()
                            is RestaurantViewModel.MenuUiState.Success -> {
                                BsSectionMenu(
                                    restaurantId = restaurant.id,
                                    restaurantName = restaurant.name,
                                    restaurantAddress = restaurant.adresse.toString(),
                                    dailyMenus = state.data,
                                    onMenuSelected = { menu ->
                                        selectedMenu = menu
                                    }
                                )
                            }
                            else -> Unit
                        }

                        Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                        BsSectionFeature(restaurant = restaurant)
                    }
                }

                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.bsSizeHeader)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val scrollOffset = if (listState.firstVisibleItemIndex > 0) {
                            headerHeightPx
                        } else {
                            listState.firstVisibleItemScrollOffset.toFloat()
                        }
                        alpha = (scrollOffset / headerHeightPx).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                sheetColor,
                                sheetColor.copy(alpha = 0f)
                            )
                        )
                    )
            )

            BottomSheetButtonClose(scope = scope, sheetState = sheetState, onDismiss = onDismiss)

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
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(R.string.error_cache),
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
                    id = if (isFavorite) R.drawable.ic_bs_heart_on else R.drawable.ic_bs_heart_off
                ),
                contentDescription = stringResource(id = R.string.bs_btn_2),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.paddingBig)
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(
                        interactionSource = favoriteInteractionSource,
                        indication = LocalIndication.current
                    ) {
                        onFavoriteClick()
                    }
                    .graphicsLayer(
                        scaleX = if (isFavoritePressed) 0.85f else 1.0f,
                        scaleY = if (isFavoritePressed) 0.85f else 1.0f,
                        alpha = if (isFavoritePressed) 0.7f else 1.0f
                    )
                    .padding(AppDimens.paddingMedium)
            )
        }
    }
}