package fr.croustillapp.visual.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.CustomFilter
import fr.croustillapp.visual.components.MainContainer
import fr.croustillapp.visual.components.SettingsContentSlider
import fr.croustillapp.visual.components.SettingsContentSwitch
import fr.croustillapp.visual.components.SettingsHeader
import fr.croustillapp.visual.dialog.DialogLocation
import fr.croustillapp.visual.restaurant.RestaurantViewModel

@Composable
fun ScreenSettings(
    viewModel: RestaurantViewModel,
    isBottomBarVisible: Boolean,
    onCloseToHomeClick: () -> Unit
) {
    val isMinimalModeEnabled by viewModel.isMinimalModeEnabled.collectAsStateWithLifecycle()

    val selectedAppLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val selectedLinkLanguage by viewModel.linkLanguage.collectAsStateWithLifecycle()

    val selectedThemeOption by viewModel.themeOption.collectAsStateWithLifecycle()
    val isPureBlackEnabled by viewModel.isPureBlackEnabled.collectAsStateWithLifecycle()
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()

    val isZoomButtons by viewModel.isZoomButtons.collectAsStateWithLifecycle()
    val isOldNamesEnabled by viewModel.isOldNamesEnabled.collectAsStateWithLifecycle()

    val isCustomLocationEnabled by viewModel.isCustomLocationEnabled.collectAsStateWithLifecycle()
    var showCustomLocationDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val urlGitHub = stringResource(R.string.url_github)
    val urlDiscord = stringResource(R.string.url_discord)

    val localizedLang = viewModel.getLocalizedUrlSegment(selectedLinkLanguage)

    val urlApp = "https://croustillant.menu/$localizedLang/mobile/android"
    val urlSite = "https://croustillant.menu/$localizedLang"

    var isFineGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var isCoarseGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    
    val updatePermissionsState = {
        isFineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        isCoarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val currentPermissionLevel = when {
        isFineGranted -> 2
        isCoarseGranted -> 1
        else -> 0
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionsState()
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshLocationPermissions()
        }
    }

    val errorToast = stringResource(R.string.error_parametres)

    val openAppSettings = remember(context) {
        {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, errorToast, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionsState()
                viewModel.checkLocationPermissionAndFetch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    val density = LocalDensity.current

    val initialHeight = 400.dp
    val minHeight = 150.dp

    val maxCollapsePx = with(density) { (initialHeight - minHeight).toPx() }

    var surfaceCollapseOffset by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    if (scrollState.value == 0 && surfaceCollapseOffset < maxCollapsePx) {
                        val oldOffset = surfaceCollapseOffset
                        surfaceCollapseOffset = (surfaceCollapseOffset - available.y).coerceIn(0f, maxCollapsePx)
                        val consumed = surfaceCollapseOffset - oldOffset
                        return Offset(0f, -consumed)
                    }
                }
                else if (available.y > 0 && scrollState.value == 0 && surfaceCollapseOffset > 0f) {
                    val oldOffset = surfaceCollapseOffset
                    surfaceCollapseOffset = (surfaceCollapseOffset - available.y).coerceIn(0f, maxCollapsePx)
                    val consumed = oldOffset - surfaceCollapseOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    MainContainer(
        isBottomBarVisible = isBottomBarVisible,
        isMinimalModeEnabled = isMinimalModeEnabled
    ) {
        val isLandscape = maxWidth > maxHeight

        LaunchedEffect(isLandscape) {
            scrollState.scrollTo(0)
        }

        val containerModifier = Modifier
            .fillMaxSize()
            .widthIn(max = AppDimens.MaxContentWidth)

        val settingsContentComposable: @Composable (Modifier) -> Unit = { settingsModifier ->
            ScreenSettingsContent(
                isOldNamesEnabled = isOldNamesEnabled,
                onToggleOldNames = { viewModel.toggleOldNames(it) },
                isZoomButtons = isZoomButtons,
                onToggleExtraInfo = { viewModel.toggleExtraInfo(it) },
                isMinimalModeEnabled = isMinimalModeEnabled,
                onToggleMinimalMode = { viewModel.toggleMinimalMode(it) },
                selectedAppLanguage = selectedAppLanguage,
                onAppLanguageSelected = { language ->
                    viewModel.setAppLanguage(language)
                },
                selectedLinkLanguage = selectedLinkLanguage,
                onLinkLanguageSelected = { viewModel.setLinkLanguage(it) },
                selectedThemeOption = selectedThemeOption,
                onThemeSelected = { viewModel.setThemeOption(it) },
                isDynamicColorEnabled = isDynamicColorEnabled,
                onToggleDynamicColor = { viewModel.toggleDynamicColor(it) },
                isPureBlackEnabled = isPureBlackEnabled,
                onTogglePureBlack = { viewModel.togglePureBlack(it) },
                isFineGranted = isFineGranted,
                isCoarseGranted = isCoarseGranted,
                onLocationPermissionChange = { newValue ->
                    updatePermissionsState()
                    val targetCran = newValue.toInt()

                    if (targetCran > currentPermissionLevel) {
                        val activity = context as? android.app.Activity
                        val showRationale = activity != null && androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )

                        when {
                            !isCoarseGranted && !isFineGranted && showRationale -> {
                                openAppSettings()
                            }
                            !isCoarseGranted -> {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                            targetCran == 2 -> {
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                )
                            }
                            else -> {
                                openAppSettings()
                            }
                        }
                    }
                },
                isCustomLocationEnabled = isCustomLocationEnabled,
                onToggleCustomLocation = { enabled ->
                    if (enabled) {
                        showCustomLocationDialog = true
                    } else {
                        viewModel.disableCustomLocation()
                    }
                },
                modifier = settingsModifier
            )
        }

        if (isLandscape) {
            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                SettingsHeader(
                    isLandscape = true,
                    isMinimalModeEnabled = isMinimalModeEnabled,
                    initialHeight = initialHeight,
                    minHeight = minHeight,
                    surfaceCollapseOffset = surfaceCollapseOffset,
                    maxCollapsePx = maxCollapsePx,
                    onCloseToHomeClick = onCloseToHomeClick,
                    urlGitHub = urlGitHub,
                    urlDiscord = urlDiscord,
                    urlApp = urlApp,
                    urlSite = urlSite,
                    modifier = Modifier.weight(0.85f).fillMaxHeight()
                )
                settingsContentComposable(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppDimens.cornerSmall))
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                )
            }
        } else {
            Column(
                modifier = containerModifier
                    .nestedScroll(nestedScrollConnection),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SettingsHeader(
                    isLandscape = false,
                    isMinimalModeEnabled = isMinimalModeEnabled,
                    initialHeight = initialHeight,
                    minHeight = minHeight,
                    surfaceCollapseOffset = surfaceCollapseOffset,
                    maxCollapsePx = maxCollapsePx,
                    onCloseToHomeClick = onCloseToHomeClick,
                    urlGitHub = urlGitHub,
                    urlDiscord = urlDiscord,
                    urlApp = urlApp,
                    urlSite = urlSite,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                settingsContentComposable(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(AppDimens.cornerSmall))
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                )
            }
        }

        if (showCustomLocationDialog) {
            DialogLocation(
                onDismissRequest = {
                    showCustomLocationDialog = false
                },
                viewModel = viewModel,
                onLocationSaved = {
                    showCustomLocationDialog = false
                }
            )
        }

    }
}

@Composable
fun ScreenSettingsContent(
    isOldNamesEnabled: Boolean,
    onToggleOldNames: (Boolean) -> Unit,
    isZoomButtons: Boolean,
    onToggleExtraInfo: (Boolean) -> Unit,
    isMinimalModeEnabled: Boolean,
    onToggleMinimalMode: (Boolean) -> Unit,
    selectedAppLanguage: String,
    onAppLanguageSelected: (String) -> Unit,
    selectedLinkLanguage: String,
    onLinkLanguageSelected: (String) -> Unit,
    selectedThemeOption: String,
    onThemeSelected: (String) -> Unit,
    isDynamicColorEnabled: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    isPureBlackEnabled: Boolean,
    onTogglePureBlack: (Boolean) -> Unit,
    isFineGranted: Boolean,
    isCoarseGranted: Boolean,
    onLocationPermissionChange: (Float) -> Unit,
    isCustomLocationEnabled: Boolean,
    onToggleCustomLocation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsContentBloc(title = stringResource(R.string.bloc_interface)) {
            SettingsContentSwitch(
                title = stringResource(R.string.switch_ergonomique),
                subtitle = stringResource(R.string.switch_ergonomique_desc),
                checked = isMinimalModeEnabled,
                onCheckedChange = onToggleMinimalMode
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsContentBloc(stringResource(R.string.bloc_langage)) {
            SettingsContentDDM(
                title = stringResource(R.string.ddm_app),
                subtitle = stringResource(R.string.ddm_app_desc),
                selectedOption = selectedAppLanguage,
                options = listOf("Auto", "Français", "Deutsch", "English", "Español"),
                onOptionSelected = onAppLanguageSelected
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsContentDDM(
                title = stringResource(R.string.ddm_link),
                subtitle = stringResource(R.string.ddm_link_desc),
                selectedOption = selectedLinkLanguage,
                options = listOf("Auto", "Français", "English"),
                onOptionSelected = onLinkLanguageSelected
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsContentBloc(title = stringResource(R.string.bloc_apparence)) {
            val systemStr = stringResource(R.string.theme_system)
            val lightStr = stringResource(R.string.theme_light)
            val darkStr = stringResource(R.string.theme_dark)

            val themeMap = mapOf(
                "system" to systemStr,
                "light" to lightStr,
                "dark" to darkStr
            )

            val currentThemeLabel = themeMap[selectedThemeOption] ?: systemStr

            SettingsContentDDM(
                title = stringResource(R.string.ddm_theme),
                subtitle = stringResource(R.string.ddm_theme_desc),
                selectedOption = currentThemeLabel,
                options = listOf(systemStr, lightStr, darkStr),
                onOptionSelected = { selectedLabel ->
                    val matchingKey = themeMap.entries.firstOrNull { it.value == selectedLabel }?.key ?: "system"
                    onThemeSelected(matchingKey)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsContentSwitch(
                title = stringResource(R.string.switch_noir),
                subtitle = stringResource(R.string.switch_noir_desc),
                checked = isPureBlackEnabled,
                onCheckedChange = onTogglePureBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsContentSwitch(
                title = stringResource(R.string.switch_dynamique),
                subtitle = stringResource(R.string.switch_dynamique_desc),
                checked = isDynamicColorEnabled,
                onCheckedChange = onToggleDynamicColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsContentBloc(title = stringResource(R.string.bloc_localisation)) {
            val sliderValue = when {
                !isFineGranted && !isCoarseGranted -> 0f
                isFineGranted -> 2f
                else -> 1f
            }

            val subtitleText = when (sliderValue.toInt()) {
                1 -> stringResource(R.string.sb_position_1)
                2 -> stringResource(R.string.sb_position_2)
                else -> stringResource(R.string.sb_position_0)
            }

            SettingsContentSlider(
                title = stringResource(R.string.sb_position),
                subtitle = if (isCustomLocationEnabled) stringResource(R.string.sb_position_X) else subtitleText,
                value = sliderValue,
                valueRange = 0f..2f,
                steps = 1,
                enabled = !isCustomLocationEnabled,
                onValueChange = { newValue ->
                    if (!isCustomLocationEnabled) {
                        onLocationPermissionChange(newValue)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsContentSwitch(
                title = stringResource(R.string.switch_manu),
                subtitle = stringResource(R.string.switch_manu_desc),
                checked = isCustomLocationEnabled,
                onCheckedChange = onToggleCustomLocation
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsContentBloc(title = stringResource(R.string.bloc_carte)) {
            SettingsContentSwitch(
                title = stringResource(R.string.switch_boutons),
                subtitle = stringResource(R.string.switch_boutons_desc),
                checked = isZoomButtons,
                onCheckedChange = onToggleExtraInfo
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsContentSwitch(
                title = stringResource(R.string.switch_noms),
                subtitle = stringResource(R.string.switch_noms_desc),
                checked = isOldNamesEnabled,
                onCheckedChange = onToggleOldNames
            )
        }
    }
}

@Composable
fun SettingsContentBloc(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Jersey10Family,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
fun SettingsContentDDM(
    title: String,
    subtitle: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(end = AppDimens.spacerBig)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            CustomFilter(
                selected = false,
                onClick = { expanded = !expanded },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ddm),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { Text(text = selectedOption) }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}