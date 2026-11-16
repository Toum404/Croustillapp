package fr.croustillapp.visual.main

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.visual.restaurant.RestaurantBottomSheet
import fr.croustillapp.visual.restaurant.RestaurantErrorBottomSheet
import fr.croustillapp.visual.restaurant.RestaurantViewModel
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: Int,
    val unselectedIcon: Int,
    val selectedIcon: Int
) {
    object Map : Screen(
        route = "map",
        title = R.string.navbar_1_map,
        unselectedIcon = R.drawable.ic_bar_carte_off,
        selectedIcon = R.drawable.ic_bar_carte_on
    )
    object Home : Screen(
        route = "home",
        title = R.string.navbar_2_home,
        unselectedIcon = R.drawable.ic_bar_home_off,
        selectedIcon = R.drawable.ic_bar_home_on
    )
    object Settings : Screen(
        route = "settings",
        title = R.string.navbar_3_settings,
        unselectedIcon = R.drawable.ic_bar_settings_off,
        selectedIcon = R.drawable.ic_bar_settings_on
    )
}
val bottomNavItems = listOf(
    Screen.Map,
    Screen.Home,
    Screen.Settings
)

@Composable
fun MainScreen(
    initialRestaurantId: String?,
    deepLinkUrl: String?
) {
    val viewModel: RestaurantViewModel = viewModel()
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { bottomNavItems.size }
    )

    val coroutineScope = rememberCoroutineScope()
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val isMinimalModeEnabled by viewModel.isMinimalModeEnabled.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val effectiveBottomBarVisible = !(isPortrait && isImeVisible) && isBottomBarVisible

    val selectedRestaurantState = rememberSaveable { mutableStateOf<Restaurant?>(null) }
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val deepLinkResto by viewModel.deepLinkRestaurant.collectAsStateWithLifecycle()
    val deepLinkErrorMsg by viewModel.deepLinkErrorEvent.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        if (initialRestaurantId != null) {
            viewModel.loadSingleRestaurantFromDeepLink(initialRestaurantId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { errorMessage ->
            Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

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

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(pagerState.currentPage) {
        keyboardController?.hide()
    }

    val mainPagerContent: @Composable () -> Unit = {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 2,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (bottomNavItems[page]) {
                Screen.Map -> {
                    ScreenMap(
                        viewModel = viewModel,
                        selectedRestaurantState = selectedRestaurantState,
                        isBottomBarVisible = effectiveBottomBarVisible,
                        onBottomBarVisibilityChanged = { isVisible ->
                            if (!isMinimalModeEnabled) {
                                isBottomBarVisible = isVisible
                            }
                        }
                    )
                }
                Screen.Home -> {
                    ScreenHome(
                        viewModel = viewModel,
                        selectedRestaurantState = selectedRestaurantState,
                        isBottomBarVisible = effectiveBottomBarVisible,
                        onBottomBarVisibilityChanged = { isVisible ->
                            if (!isMinimalModeEnabled) {
                                isBottomBarVisible = isVisible
                            }
                        },
                        onNavigateToSettings = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                }
                Screen.Settings -> {
                    ScreenSettings(
                        viewModel = viewModel,
                        isBottomBarVisible = isBottomBarVisible,
                        onCloseToHomeClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isLandscape = maxWidth > maxHeight

        val shouldShowNav = if (isLandscape) {
            !isMinimalModeEnabled
        } else {
            isBottomBarVisible && !isMinimalModeEnabled
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = shouldShowNav,
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .width(AppDimens.barGraisseHorizontal)
                            .fillMaxHeight()
                            .heightIn(max = 600.dp)
                            .padding(vertical = AppDimens.barPadding, horizontal = 12.dp),
                        shape = RoundedCornerShape(AppDimens.cornerBig),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            bottomNavItems.forEachIndexed { index, screen ->
                                val isSelected = pagerState.currentPage == index
                                FloatingNavItem(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    screen = screen,
                                    isSelected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    mainPagerContent()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                mainPagerContent()

                AnimatedVisibility(
                    visible = shouldShowNav,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spacerBig),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavItems.forEachIndexed { index, screen ->
                                val isSelected = pagerState.currentPage == index
                                FloatingNavItem(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    screen = screen,
                                    isSelected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedRestaurantState.value?.let { restaurant ->
            RestaurantBottomSheet(
                restaurant = restaurant,
                isFavorite = restaurant.id in favoriteIds,
                onFavoriteClick = { viewModel.toggleFavorite(restaurant.id) },
                onDismiss = { selectedRestaurantState.value = null }
            )
        }

        deepLinkErrorMsg?.let { errorResId ->
            RestaurantErrorBottomSheet(
                message = stringResource(id = errorResId),
                deepLinkUrl = deepLinkUrl ?: "",
                onDismiss = { viewModel.clearDeepLinkError() },
                onInjectIdToSearch = { restaurantId ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                    viewModel.updateSearchText(restaurantId)
                }
            )
        }
    }
}

@Composable
fun FloatingNavItem(
    modifier: Modifier = Modifier,
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val iconResId = if (isSelected) screen.selectedIcon else screen.unselectedIcon

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(AppDimens.cornerBig),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = stringResource(id = screen.title),
                tint = contentColor
            )
        }
    }
}