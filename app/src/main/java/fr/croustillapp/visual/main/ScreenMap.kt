package fr.croustillapp.visual.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.croustillapp.R
import fr.croustillapp.data.model.GeoJsonFeatureCollection
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.data.translator.getOldRegionName
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.CustomFilter
import fr.croustillapp.visual.components.MainContainer
import fr.croustillapp.visual.components.MapCanvas
import fr.croustillapp.visual.components.RestaurantList
import fr.croustillapp.visual.restaurant.RestaurantViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class TerritoryBound(
    val key: String,
    val bounds: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TerritoryBound
        if (key != other.key) return false
        if (!bounds.contentEquals(other.bounds)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + bounds.contentHashCode()
        return result
    }
}

private val OVERSEAS_TERRITORIES = listOf(
    TerritoryBound("guadeloupe", doubleArrayOf(-62.0, 15.7, -60.9, 16.6)),
    TerritoryBound("martinique", doubleArrayOf(-61.3, 14.3, -60.7, 15.0)),
    TerritoryBound("guyane", doubleArrayOf(-55.0, 2.0, -51.0, 6.0)),
    TerritoryBound("reunion", doubleArrayOf(55.0, -21.5, 56.0, -20.8)),
    TerritoryBound("mayotte", doubleArrayOf(44.9, -13.1, 45.4, -12.5))
)

@Composable
fun ScreenMap(
    viewModel: RestaurantViewModel,
    selectedRestaurantState: MutableState<Restaurant?>,
    isBottomBarVisible: Boolean,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val geoJsonData by viewModel.geoJsonData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isMapLoading.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val isOldNamesEnabled by viewModel.isOldNamesEnabled.collectAsStateWithLifecycle()
    val selectedCrousId by viewModel.selectedCrousId.collectAsStateWithLifecycle()
    val regionalRestaurants by viewModel.restaurantsForSelectedCrousAndZone.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val newRestaurantIds by viewModel.newRestaurantIds.collectAsStateWithLifecycle()
    val isPrecisionExact by viewModel.isPrecisionExact.collectAsStateWithLifecycle()
    val availableZones by viewModel.availableZones.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()
    val isMinimalModeEnabled by viewModel.isMinimalModeEnabled.collectAsStateWithLifecycle()

    var isMapVisible by remember { mutableStateOf(true) }
    var userHasScrolledUp by remember { mutableStateOf(false) }
    var forceExpandFromRegionHandle by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val selectedRegionName = remember(selectedCrousId, geoJsonData, isOldNamesEnabled) {
        if (selectedCrousId == null) {
            "Aucune sélection"
        } else {
            val feature = geoJsonData?.features?.find { it.properties.crousId == selectedCrousId }
            val defaultName = feature?.properties?.crousNom ?: "Région #${selectedCrousId}"
            if (isOldNamesEnabled) getOldRegionName(selectedCrousId, defaultName) else defaultName
        }
    }

    LaunchedEffect(selectedZone) {
        gridState.animateScrollToItem(0)
    }

    MainContainer(
        isBottomBarVisible = isBottomBarVisible,
        isMinimalModeEnabled = isMinimalModeEnabled
    ) {
        val containerModifier = Modifier
            .fillMaxSize()
            .widthIn(max = AppDimens.MaxContentWidth)

        if (isLandscape) {
            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapCanvasSection(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    viewModel = viewModel,
                    geoJsonData = geoJsonData,
                    isLoading = isLoading,
                    userLocation = userLocation,
                    selectedCrousId = selectedCrousId
                )
                MapDetailsSection(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selectedRegionName = selectedRegionName,
                    selectedCrousId = selectedCrousId,
                    availableZones = availableZones,
                    selectedZone = selectedZone,
                    regionalRestaurants = regionalRestaurants,
                    favoriteIds = favoriteIds,
                    newRestaurantIds = newRestaurantIds,
                    isPrecisionExact = isPrecisionExact,
                    gridState = gridState,
                    viewModel = viewModel,
                    selectedRestaurantState = selectedRestaurantState,
                    userHasScrolledUp = userHasScrolledUp,
                    onUserHasScrolledUpChange = { userHasScrolledUp = it },
                    onForceExpandChange = { forceExpandFromRegionHandle = it },
                    coroutineScope = coroutineScope
                )
            }
        } else {
            val portraitNestedScrollConnection = remember(gridState) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (available.y < 0 && isMapVisible) {
                            val isAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                            if (isAtTop) {
                                isMapVisible = false
                                onBottomBarVisibilityChanged(false)
                                return Offset(0f, available.y)
                            }
                        } else if (available.y > 0 && !isMapVisible) {
                            val isAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                            if (isAtTop) {
                                isMapVisible = true
                                onBottomBarVisibilityChanged(true)
                                return Offset(0f, available.y)
                            }
                        }
                        return Offset.Zero
                    }
                }
            }

            Column(
                modifier = containerModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = isMapVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MapCanvasSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(bottom = 16.dp),
                        viewModel = viewModel,
                        geoJsonData = geoJsonData,
                        isLoading = isLoading,
                        userLocation = userLocation,
                        selectedCrousId = selectedCrousId
                    )
                }

                MapDetailsSection(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(portraitNestedScrollConnection),
                    selectedRegionName = selectedRegionName,
                    selectedCrousId = selectedCrousId,
                    availableZones = availableZones,
                    selectedZone = selectedZone,
                    regionalRestaurants = regionalRestaurants,
                    favoriteIds = favoriteIds,
                    newRestaurantIds = newRestaurantIds,
                    isPrecisionExact = isPrecisionExact,
                    gridState = gridState,
                    viewModel = viewModel,
                    selectedRestaurantState = selectedRestaurantState,
                    userHasScrolledUp = userHasScrolledUp,
                    onUserHasScrolledUpChange = { userHasScrolledUp = it },
                    onForceExpandChange = { forceExpandFromRegionHandle = it },
                    coroutineScope = coroutineScope
                )
            }
        }
    }
}

@Composable
private fun MapCanvasSection(
    modifier: Modifier,
    viewModel: RestaurantViewModel,
    geoJsonData: GeoJsonFeatureCollection?,
    isLoading: Boolean,
    userLocation: Pair<Double, Double>?,
    selectedCrousId: Int?
) {
    MapCanvas(
        modifier = modifier,
        viewModel = viewModel,
        geoJsonData = geoJsonData,
        isLoading = isLoading,
        userLocation = userLocation,
        selectedCrousId = selectedCrousId,
        overseasTerritories = OVERSEAS_TERRITORIES
    )
}

@Composable
private fun MapDetailsSection(
    modifier: Modifier,
    selectedRegionName: String,
    selectedCrousId: Int?,
    availableZones: List<String>,
    selectedZone: String?,
    regionalRestaurants: List<Restaurant>,
    favoriteIds: Set<String>,
    newRestaurantIds: Set<String>,
    isPrecisionExact: Boolean,
    gridState: LazyGridState,
    viewModel: RestaurantViewModel,
    selectedRestaurantState: MutableState<Restaurant?>,
    userHasScrolledUp: Boolean,
    onUserHasScrolledUpChange: (Boolean) -> Unit,
    onForceExpandChange: (Boolean) -> Unit,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = selectedRegionName,
            style = MaterialTheme.typography.titleMedium,
            color = if (selectedCrousId != null && availableZones.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            fontFamily = Jersey10Family,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (selectedCrousId != null) {
                        viewModel.onZoneSelected(null)
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    }
                }
                .pointerInput(userHasScrolledUp) {
                    if (userHasScrolledUp) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 10f) {
                                    onForceExpandChange(true)
                                    onUserHasScrolledUpChange(false)
                                    coroutineScope.launch { gridState.scrollToItem(0) }
                                }
                            }
                        )
                    }
                }
        )

        if (selectedCrousId != null && availableZones.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.cornerSmall)),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spacerMedium, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(vertical = AppDimens.spacerMedium),
            ) {
                items(availableZones) { zone ->
                    CustomFilter(
                        selected = (selectedZone == zone),
                        onClick = { viewModel.onZoneSelected(zone) },
                        label = { Text(text = zone) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        if (selectedCrousId != null) {
            RestaurantList(
                restaurants = regionalRestaurants,
                isLoading = false,
                favoriteIds = favoriteIds,
                newRestaurantIds = newRestaurantIds,
                isPrecisionExact = isPrecisionExact,
                onRestaurantClick = { restaurant -> selectedRestaurantState.value = restaurant },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.cornerSmall))
                    .background(MaterialTheme.colorScheme.background),
                listState = gridState
            )
        } else {
            EmptyMapStateBox()
        }
    }
}

@Composable
private fun EmptyMapStateBox() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cornerSmall)),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = RoundedCornerShape(AppDimens.cornerSmall),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_map_empty),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(AppDimens.spacerBig))
                Text(
                    text = stringResource(R.string.carte_vide_titre),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppDimens.spacerMedium))
                Text(
                    text = stringResource(R.string.carte_vide_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}