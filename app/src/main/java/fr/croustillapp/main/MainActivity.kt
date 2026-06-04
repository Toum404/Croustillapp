package fr.croustillapp.main

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.features.bottomsheet.BottomSheetInformation
import fr.croustillapp.features.bottomsheet.RestaurantBottomSheet
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.features.data.getTranslationForType
import fr.croustillapp.features.elements.RestaurantList
import fr.croustillapp.features.elements.RestaurantViewModel
import fr.croustillapp.ui.theme.CroustillappTheme
import fr.croustillapp.ui.theme.Jersey10Family

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FR: Extraction de l'ID du restaurant depuis le Deep Link (Intent) à l'ouverture de l'app.
        // EN: Extracting the restaurant ID from the Deep Link (Intent) upon app initialization.
        val intentData: Uri? = intent?.data
        val initialRestaurantId = intentData?.let { uri ->
            extractRestaurantIdFromUrl(uri.toString())
        }

        enableEdgeToEdge()

        setContent {
            CroustillappTheme {
                val viewModel: RestaurantViewModel = viewModel()

                // FR: États persistants lors des changements de configuration (ex: rotation d'écran).
                // EN: Persistent states across configuration changes (e.g., screen rotation).
                val showInformationState = rememberSaveable { mutableStateOf(false) }
                val selectedRestaurantState = rememberSaveable { mutableStateOf<Restaurant?>(null) }

                // FR: Collecte des flux d'état réactifs du ViewModel respectant le cycle de vie Android.
                // EN: Collecting reactive state flows from the ViewModel in a lifecycle-aware manner.
                val restaurants by viewModel.filteredRestaurants.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val errorType by viewModel.errorType.collectAsStateWithLifecycle()
                val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

                val deepLinkResto by viewModel.deepLinkRestaurant.collectAsStateWithLifecycle()
                val appContext = LocalContext.current.applicationContext

                // FR: Déclenche le chargement des données si l'application s'ouvre via un Deep Link.
                // EN: Triggers data loading if the application opens via a Deep Link.
                LaunchedEffect(Unit) {
                    if (intent?.data != null) {
                        viewModel.loadSingleRestaurantFromDeepLink(initialRestaurantId)
                    }
                }

                // FR: Écouteur d'événements unique pour afficher les Toasts natifs d'erreurs globales.
                // EN: Single event listener to display native global error Toasts.
                LaunchedEffect(Unit) {
                    viewModel.errorEvents.collect { stringResId ->
                        val errorMessage = appContext.getString(stringResId)
                        Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                // FR: Ouvre automatiquement la BottomSheet du restaurant récupéré par le Deep Link.
                // EN: Automatically displays the BottomSheet of the restaurant retrieved via the Deep Link.
                LaunchedEffect(deepLinkResto) {
                    deepLinkResto?.let { resto ->
                        selectedRestaurantState.value = resto
                        viewModel.clearDeepLinkRestaurant()
                    }
                }

                // FR: Récupération des filtres de l'interface utilisateur.
                // EN: Fetching user interface filter properties.
                val searchText by viewModel.searchText.collectAsStateWithLifecycle()
                val showOnlyOpen by viewModel.showOnlyOpen.collectAsStateWithLifecycle()
                val showOnlyPmr by viewModel.showOnlyPmr.collectAsStateWithLifecycle()
                val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
                val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()

                var isTypeMenuExpanded by remember { mutableStateOf(false) }
                var isRegionMenuExpanded by remember { mutableStateOf(false) }

                val typesList by viewModel.typesList.collectAsStateWithLifecycle()
                val regionsList by viewModel.regionsList.collectAsStateWithLifecycle()

                val listState = rememberLazyGridState()

                // FR: Configuration par défaut pour les puces de filtrage (Chips).
                // EN: Default styling configuration for UI FilterChips.
                val chipBorder = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = true,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 0.dp
                )

                val chipColors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                val isPrecisionExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                                .padding(top = 16.dp)
                        ) {
                            val keyboardController = LocalSoftwareKeyboardController.current
                            val focusManager = LocalFocusManager.current

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { viewModel.updateSearchText(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(id = R.string.recherche), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Box(modifier = Modifier.padding(start = 8.dp)) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_main_search),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (searchText.isNotEmpty()) {
                                            IconButton(
                                                onClick = { viewModel.updateSearchText("") },
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_main_clear),
                                                    contentDescription = stringResource(id = R.string.action_effacer),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { showInformationState.value = true },
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_main_information),
                                                    contentDescription = stringResource(id = R.string.description_informations),
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    modifier = Modifier.weight(0.80f),
                                    selected = showOnlyOpen,
                                    onClick = { viewModel.toggleOnlyOpen(!showOnlyOpen) },
                                    border = chipBorder,
                                    colors = chipColors,
                                    label = {
                                        Text(
                                            text = stringResource(id = R.string.statut_ouvert),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )

                                FilterChip(
                                    modifier = Modifier.weight(0.70f),
                                    selected = showOnlyPmr,
                                    onClick = { viewModel.toggleOnlyPmr(!showOnlyPmr) },
                                    border = chipBorder,
                                    colors = chipColors,
                                    label = {
                                        Text(
                                            text = stringResource(id = R.string.statut_pmr),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    FilterChip(
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = selectedType != "Tous",
                                        onClick = { isTypeMenuExpanded = true },
                                        border = chipBorder,
                                        colors = chipColors,
                                        label = {
                                            Text(
                                                text = if (selectedType == "Tous") stringResource(R.string.label_type) else getTranslationForType(selectedType),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_ddm),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = isTypeMenuExpanded,
                                        onDismissRequest = { isTypeMenuExpanded = false }
                                    ) {
                                        typesList.forEach { typeLabel ->
                                            DropdownMenuItem(
                                                text = { Text(getTranslationForType(typeLabel)) },
                                                onClick = {
                                                    viewModel.updateType(typeLabel)
                                                    isTypeMenuExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (selectedType == typeLabel) {
                                                        Icon(painter = painterResource(id = R.drawable.ic_visual_check), contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    FilterChip(
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = selectedRegion != "Toutes",
                                        onClick = { isRegionMenuExpanded = true },
                                        border = chipBorder,
                                        colors = chipColors,
                                        label = {
                                            Text(
                                                text = if (selectedRegion == "Toutes") stringResource(R.string.label_region) else selectedRegion,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_ddm),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = isRegionMenuExpanded,
                                        onDismissRequest = { isRegionMenuExpanded = false }
                                    ) {
                                        regionsList.forEach { regionName ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (regionName == "Toutes") stringResource(R.string.filtre_tous) else regionName)
                                                },
                                                onClick = {
                                                    viewModel.updateRegion(regionName)
                                                    isRegionMenuExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (selectedRegion == regionName) {
                                                        Icon(painter = painterResource(id = R.drawable.ic_visual_check), contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(0.dp)
                                .navigationBarsPadding()
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.api_mention))
                                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                        append(stringResource(R.string.api_nom))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.Center),
                                fontFamily = Jersey10Family,
                                fontSize = 12.sp
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            RestaurantList(
                                restaurants = restaurants,
                                isLoading = isLoading,
                                errorType = errorType,
                                favoriteIds = favoriteIds,
                                isPrecisionExact = isPrecisionExact,
                                onRestaurantClick = { selectedRestaurantState.value = it },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp),
                                listState = listState
                            )
                        }
                    }

                    if (showInformationState.value) {
                        BottomSheetInformation(
                            onDismiss = { showInformationState.value = false },
                            onPermissionGranted = {
                                viewModel.checkLocationPermissionAndFetch()
                            }
                        )
                    }

                    selectedRestaurantState.value?.let { restaurant ->
                        RestaurantBottomSheet(
                            restaurant = restaurant,
                            isFavorite = restaurant.id in favoriteIds,
                            onFavoriteClick = { viewModel.toggleFavorite(restaurant.id) },
                            onDismiss = { selectedRestaurantState.value = null }
                        )
                    }
                }

                val isFirstLoadState = rememberSaveable { mutableStateOf(true) }

                // FR: Force le défilement de la liste vers le haut lors d'une mise à jour des filtres.
                // EN: Forces the list to scroll back to top whenever active filter outputs update.
                LaunchedEffect(restaurants) {
                    if (isFirstLoadState.value) {
                        isFirstLoadState.value = false
                    } else {
                        listState.scrollToItem(0)
                    }
                }
            }
        }
    }
}

/**
 * FR: Extrait l'ID unique du restaurant depuis l'URL de l'application.
 * EN: Extracts the unique venue ID from deep-linked application context URLs.
 */
fun extractRestaurantIdFromUrl(url: String?): String? {
    if (url == null) return null

    val regex = Regex("-r(\\d+)/?$|/r(\\d+)/?$")
    val matchResult = regex.find(url)

    return matchResult?.groupValues?.lastOrNull { it.isNotEmpty() && it != matchResult.value }
}