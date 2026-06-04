package fr.croustillapp.features.bottomsheet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import fr.croustillapp.R
import fr.croustillapp.ui.theme.Jersey10Family
import kotlinx.coroutines.launch

/**
 * FR: Feuille de modale affichant les informations de l'application et la configuration des permissions.
 * EN: Modal BottomSheet presenting application details and handling geolocation permission setups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetInformation(onDismiss: () -> Unit, onPermissionGranted: () -> Unit) {

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val scope = rememberCoroutineScope()
    val sheetColor = BottomSheetDefaults.ContainerColor
    val context = LocalContext.current

    val webSiteUrl = stringResource(R.string.url_site_web)
    val gitHubUrl = stringResource(R.string.url_github)
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

    // FR: Synchronisation réactive de l'état des permissions lorsque l'utilisateur revient sur l'application.
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
                        onPermissionGranted()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // FR: Lanceur d'activité asynchrone pour la demande simultanée de plusieurs permissions.
    // EN: Activity result launcher handling multiple concurrent asynchronous permission requests.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (isCoarseGranted || isFineGranted) {
            triggerRadarAnimation = true
            onPermissionGranted()
        }
    }

    // FR: Intent explicite permettant de rediriger l'utilisateur vers la page de configuration système de l'application.
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
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    // FR: Utilisation de ImageDecoderDecoder pour prendre en charge nativement les formats d'images spécifiques (webp).
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
                    Text(
                        text = stringResource(R.string.description_informations),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Jersey10Family,
                        fontSize = 36.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val appName = stringResource(R.string.app_name)
                    val apiNom = stringResource(R.string.api_nom)
                    val crousMention = stringResource(R.string.crous_mention)
                    val fullText = stringResource(R.string.description_app, appName, apiNom, crousMention)

                    Text(
                        text = fullText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Justify
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InformationCard(
                        title = stringResource(R.string.service_nom),
                        description = stringResource(R.string.service_description),
                        buttonText = stringResource(R.string.action_visiter, apiNom),
                        onButtonClick = { openUrl(webSiteUrl) },
                        modifier = Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ),
                        iconSlot = {
                            Image(
                                painter = painterResource(R.drawable.img_api),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // FR: Adaptation dynamique des bordures et arrière-plans de la carte selon l'état de la permission.
                    // EN: Dynamic container outline adaptation depending entirely on location permission compliance.
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
                            isFineGranted -> stringResource(R.string.location_card_desc_fine, appName)
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

                                    // FR: Routage intelligent : demande la permission via l'OS, ou redirige vers les paramètres si l'utilisateur a déjà refusé définitivement.
                                    // EN: Smart routing logic: requests runtime permission or falls back to system settings if previously denied permanently.
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
                        // FR: Animation fluide de fermeture de la BottomSheet avant de notifier l'UI globale.
                        // EN: Animated programmatic dismissal sequence running prior to invoking final external callbacks.
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    }
                    .padding(8.dp)
            )

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

/**
 * FR: Sous-composant réutilisable pour structurer les cartes d'informations.
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
 * FR: Composant d'icône animée encapsulant un ImageView traditionnel pour exécuter une séquence d'images (flipbook).
 * EN: Animated icon component encapsulating a legacy ImageView to execute an image-by-image sequence (flipbook).
 */
@Composable
fun AnimatedRadarIcon(
    isTriggered: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    // FR: Rétention locale de la référence de la vue native pour interagir avec le framework de drawables classiques.
    // EN: Local retention of the native view reference to interact with the legacy drawable framework.
    val imageView = remember { mutableStateOf<ImageView?>(null) }

    // FR: Déclenchement réactif de l'animation d'images lorsque le signal d'activation passe à vrai.
    // EN: Reactive execution of the frame animation sequence when the trigger signal transitions to true.
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            val drawable = imageView.value?.drawable as? AnimationDrawable
            drawable?.stop()
            drawable?.start()
        }
    }

    // FR: Interopérabilité : Intégration et mise à jour d'un composant UI Android natif au sein du moteur Compose.
    // EN: Interoperability: Embedding and updating a native Android UI component inside the Compose engine.
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