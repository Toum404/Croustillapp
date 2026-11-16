package fr.croustillapp.visual.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.visual.components.EmptyState
import fr.croustillapp.visual.components.HomeFilterRow
import fr.croustillapp.visual.components.HomeSearchBar
import fr.croustillapp.visual.components.MainContainer
import fr.croustillapp.visual.components.RestaurantList
import fr.croustillapp.visual.restaurant.RestaurantViewModel

@Composable
fun ScreenHome(
    viewModel: RestaurantViewModel,
    selectedRestaurantState: MutableState<Restaurant?>,
    isBottomBarVisible: Boolean,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val isMinimalModeEnabled by viewModel.isMinimalModeEnabled.collectAsStateWithLifecycle()

    val actionInteractionSource = remember { MutableInteractionSource() }

    val restaurants by viewModel.filteredRestaurants.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val newRestaurantIds by viewModel.newRestaurantIds.collectAsStateWithLifecycle()

    val deepLinkResto by viewModel.deepLinkRestaurant.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext

    // FR: Ecouteur d'evenements unique pour afficher les Toasts natifs d'erreurs globales.
    // EN: Single event listener to display native global error Toasts.
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { errorMessage ->
            Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    // FR: Ouvre automatiquement la BottomSheet du restaurant recupere par le Deep Link.
    // EN: Automatically displays the BottomSheet of the restaurant retrieved via the Deep Link.
    LaunchedEffect(deepLinkResto) {
        deepLinkResto?.let { resto ->
            selectedRestaurantState.value = resto
            viewModel.clearDeepLinkError()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkLocationPermissionAndFetch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val showOnlyOpen by viewModel.showOnlyOpen.collectAsStateWithLifecycle()
    val showOnlyPmr by viewModel.showOnlyPmr.collectAsStateWithLifecycle()
    val showOnlyIzly by viewModel.showOnlyIzly.collectAsStateWithLifecycle()
    val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()

    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isRegionMenuExpanded by remember { mutableStateOf(false) }

    val typesList by viewModel.typesList.collectAsStateWithLifecycle()
    val regionsList by viewModel.regionsList.collectAsStateWithLifecycle()

    val listState = rememberLazyGridState()

    val isFiltered = remember(searchText, showOnlyOpen, showOnlyPmr, showOnlyIzly, selectedRegion, selectedType) {
        searchText.isNotEmpty() ||
                showOnlyOpen ||
                showOnlyPmr ||
                showOnlyIzly ||
                (selectedRegion != "Toutes") ||
                (selectedType != "Tous")
    }

    val isPrecisionExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()

    MainContainer(
        isBottomBarVisible = isBottomBarVisible,
        isMinimalModeEnabled = isMinimalModeEnabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = AppDimens.MaxContentWidth)
        ) {
            HomeSearchBar(
                searchText = searchText,
                isMinimalModeEnabled = isMinimalModeEnabled,
                actionInteractionSource = actionInteractionSource,
                onSearchTextChanged = { viewModel.updateSearchText(it) },
                onNavigateToSettings = onNavigateToSettings
            )

            HomeFilterRow(
                showOnlyOpen = showOnlyOpen,
                showOnlyPmr = showOnlyPmr,
                showOnlyIzly = showOnlyIzly,
                selectedRegion = selectedRegion,
                selectedType = selectedType,
                regionsList = regionsList,
                typesList = typesList,
                isRegionMenuExpanded = isRegionMenuExpanded,
                isTypeMenuExpanded = isTypeMenuExpanded,
                onRegionMenuExpandedChange = { isRegionMenuExpanded = it },
                onTypeMenuExpandedChange = { isTypeMenuExpanded = it },
                viewModel = viewModel
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .clip(RoundedCornerShape(AppDimens.cornerSmall))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    isInitialLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    isFiltered && restaurants.isEmpty() -> {
                        EmptyState(modifier = Modifier.fillMaxSize())
                    }

                    else -> {
                        RestaurantList(
                            restaurants = restaurants,
                            isLoading = isLoading,
                            favoriteIds = favoriteIds,
                            newRestaurantIds = newRestaurantIds,
                            isPrecisionExact = isPrecisionExact,
                            onRestaurantClick = { selectedRestaurantState.value = it },
                            modifier = Modifier.fillMaxSize(),
                            listState = listState
                        )
                    }
                }
            }

            val isFirstLoadState = rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(searchText, showOnlyOpen, showOnlyPmr, showOnlyIzly, selectedRegion, selectedType) {
                if (isFirstLoadState.value) {
                    isFirstLoadState.value = false
                } else {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    val isFirstLoadState = rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(showOnlyOpen, showOnlyPmr, showOnlyIzly, selectedRegion, selectedType) {
        if (isFirstLoadState.value) {
            isFirstLoadState.value = false
        } else {
            listState.animateScrollToItem(0)
            onBottomBarVisibilityChanged(true)
        }
    }
}