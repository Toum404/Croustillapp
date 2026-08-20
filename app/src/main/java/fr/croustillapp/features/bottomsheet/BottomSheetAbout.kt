package fr.croustillapp.features.bottomsheet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import fr.croustillapp.R
import fr.croustillapp.features.elements.RestaurantViewModel
import fr.croustillapp.ui.theme.Jersey10Family
import kotlinx.coroutines.launch

/**
 * FR: Feuille de modale affichant les informations de l'application et la configuration des permissions.
 * EN: Modal BottomSheet presenting application details and handling geolocation permission setups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetInformation(
    viewModel: RestaurantViewModel = viewModel(),
    onDismiss: () -> Unit,
    onPermissionGranted: () -> Unit) {

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val isSheetStable = sheetState.currentValue == sheetState.targetValue
    val scrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    val sheetColor = BottomSheetDefaults.ContainerColor
    val context = LocalContext.current

    val showGpsOffAlert by viewModel.showGpsOffAlert.collectAsState()

    val gitHubUrl = stringResource(R.string.url_github)
    val discordUrl = stringResource(R.string.url_discord)
    val webSiteUrl = stringResource(R.string.url_api)
    val appUrl = stringResource(R.string.url_application)

    val erreurAction = stringResource(R.string.url_action_erreur)

    val closeInteractionSource = remember { MutableInteractionSource() }
    val gitInteractionSource = remember { MutableInteractionSource() }

    val isClosePressed by closeInteractionSource.collectIsPressedAsState()
    val isGitPressed by gitInteractionSource.collectIsPressedAsState()

    var isFineGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var isCoarseGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    var triggerRadarAnimation by remember { mutableStateOf(false) }

    val onlyHasCoarse = isCoarseGranted && !isFineGranted

    // FR: Synchronisation reactive de l'etat des permissions lorsque l'utilisateur revient sur l'application.
    // EN: Reactive sync hook tracking authorization states when the application returns to foreground execution.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (fine != isFineGranted || coarse != isCoarseGranted) {
                    isFineGranted = fine
                    isCoarseGranted = coarse
                    if (fine || coarse) {
                        triggerRadarAnimation = true
                        viewModel.refreshLocationPermissions()
                        onPermissionGranted()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // FR: Lanceur d'activite asynchrone pour la demande simultanee de plusieurs permissions.
    // EN: Activity result launcher handling multiple concurrent asynchronous permission requests.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (isCoarseGranted || isFineGranted) {
            triggerRadarAnimation = true
            viewModel.refreshLocationPermissions()
            onPermissionGranted()
        }
    }

    // FR: Intent explicite permettant de rediriger l'utilisateur vers la page de configuration systeme de l'application.
    // EN: Explicit Intent routing users directly to the native OS settings panel for this application.
    val openAppSettings = remember(context) {
        {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, erreurAction, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openUrl = remember(context, erreurAction) {
        { url: String ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, erreurAction, Toast.LENGTH_SHORT).show()
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
                ) {
                    // FR: Utilisation de ImageDecoderDecoder pour prendre en charge nativement les formats d'images specifiques (webp).
                    // EN: Leveraging ImageDecoderDecoder to support special image decoding pipelines natively (webp).
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.drawable.img_illustration)
                            .decoderFactory(ImageDecoderDecoder.Factory())
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    PixelNoiseFade(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.BottomCenter),
                        color = sheetColor
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val apiNom = stringResource(R.string.api_nom)
                    val appName = stringResource(R.string.app_name)
                    val crousMention = stringResource(R.string.crous_mention)
                    val fullText = stringResource(R.string.description_app, appName, apiNom, crousMention)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.description_informations),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = Jersey10Family,
                            fontSize = 32.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = fullText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Justify
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )

                            Text(
                                text = apiNom,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EpurButton(
                                label = stringResource(R.string.info_api_discord),
                                iconOutlinedRes = R.drawable.ic_info_discord_outlined,
                                iconFilledRes = R.drawable.ic_info_discord_filled,
                                modifier = Modifier.weight(1f),
                                onClick = { openUrl(discordUrl) }
                            )

                            EpurButton(
                                label = stringResource(R.string.info_api_website),
                                iconOutlinedRes = R.drawable.ic_info_api_outlined,
                                iconFilledRes = R.drawable.ic_info_api_filled,
                                modifier = Modifier.weight(1f),
                                onClick = { openUrl(webSiteUrl) }
                            )

                            EpurButton(
                                label = stringResource(R.string.info_api_app),
                                iconOutlinedRes = R.drawable.ic_info_android_outlined,
                                iconFilledRes = R.drawable.ic_info_android_filled,
                                modifier = Modifier.weight(1f),
                                onClick = { openUrl(appUrl) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val cardShape = RoundedCornerShape(8.dp)
                    val locationModifier = when {
                        isFineGranted -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), cardShape)
                        onlyHasCoarse -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, cardShape)
                        else -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, cardShape)
                    }

                    InformationCard(
                        title = when {
                            isFineGranted -> stringResource(R.string.location_card_title_fine)
                            onlyHasCoarse -> stringResource(R.string.location_card_title_coarse)
                            else -> stringResource(R.string.location_card_title_none)
                        },
                        description = when {
                            isFineGranted -> stringResource(R.string.location_card_desc_fine, stringResource(R.string.app_name))
                            onlyHasCoarse -> stringResource(R.string.location_card_desc_coarse)
                            else -> stringResource(R.string.location_card_desc_none)
                        },
                        buttonText = when {
                            isFineGranted -> stringResource(R.string.location_card_button_fine)
                            onlyHasCoarse -> stringResource(R.string.location_card_button_coarse)
                            else -> stringResource(R.string.location_card_button_none)
                        },
                        buttonEnabled = !isFineGranted,
                        onButtonClick = {
                            if (onlyHasCoarse) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            } else {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    val showRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )

                                    when {
                                        !isCoarseGranted && !showRationale -> {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        }
                                        else -> {
                                            openAppSettings()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = locationModifier,
                        titleColor = if (isFineGranted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        descriptionColor = if (isFineGranted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        buttonColors = ButtonDefaults.buttonColors(
                            containerColor = if (onlyHasCoarse) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = if (onlyHasCoarse) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        iconSlot = {
                            AnimatedRadarIcon(
                                isTriggered = triggerRadarAnimation,
                                tint = when {
                                    isFineGranted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    onlyHasCoarse -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_bs_close),
                contentDescription = stringResource(R.string.action_fermer),
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
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
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
                    visible = showGpsOffAlert,
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
                            text = stringResource(R.string.bts_position),
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
                    id = if (isGitPressed) R.drawable.ic_bs_git_filled else R.drawable.ic_bs_git_outlined
                ),
                contentDescription = stringResource(R.string.url_github),
                tint = Color(0xFF9155FD), // Special GitHub
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = gitInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current
                    ) {
                        openUrl(gitHubUrl)
                    }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun EpurButton(
    label: String,
    iconFilledRes: Int,
    iconOutlinedRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isPressed) iconFilledRes else iconOutlinedRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * FR: Sous-composant reutilisable pour structurer les cartes d'informations.
 * EN: Modular sub-component layout utilized for structuring information card layouts.
 */
@Composable
fun InformationCard(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonEnabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    buttonColors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    iconSlot: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            iconSlot()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontFamily = Jersey10Family, fontSize = 22.sp, color = titleColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, fontSize = 13.sp, lineHeight = 17.sp, color = descriptionColor, textAlign = TextAlign.Center, minLines = 3, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onButtonClick, enabled = buttonEnabled, colors = buttonColors, elevation = null, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * FR: Composant d'icônes animee encapsulant un ImageView traditionnel pour executer une sequence d'images (flipbook).
 * EN: Animated icon component encapsulating a legacy ImageView to execute an image-by-image sequence (flipbook).
 */
@Composable
fun AnimatedRadarIcon(
    isTriggered: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val imageView = remember { mutableStateOf<ImageView?>(null) }

    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            val drawable = imageView.value?.drawable as? AnimationDrawable
            drawable?.stop()
            drawable?.start()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_anim_radar)
                setImageDrawable(drawable)

                setColorFilter(tint.toArgb())
                alpha = tint.alpha

                imageView.value = this
            }
        },
        update = { view ->
            view.setColorFilter(tint.toArgb())
            view.alpha = tint.alpha
        }
    )
}