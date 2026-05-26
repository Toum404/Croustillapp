package fr.croustillapp.features

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import fr.croustillapp.components.EmptyState
import fr.croustillapp.components.NoInternetState
import fr.croustillapp.components.RestaurantCard
import fr.croustillapp.components.RestaurantCardSkeleton
import fr.croustillapp.components.rememberShimmerBrush
import fr.croustillapp.data.Restaurant

@Composable
fun RestaurantList(
    restaurants: List<Restaurant>,
    isLoading: Boolean,
    isError: Boolean,
    favoriteIds: Set<String>,
    onRestaurantClick: (Restaurant) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyGridState = rememberLazyGridState()
) {

    val shimmerBrush = rememberShimmerBrush()

    val orientation = LocalConfiguration.current.orientation
    val columns by remember(orientation) {
        derivedStateOf { if (orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2 }
    }

    val sortedRestaurants by remember(restaurants, favoriteIds) {
        derivedStateOf {
            restaurants.sortedWith(
                compareByDescending<Restaurant> { it.id in favoriteIds }
                    .thenBy { it.name }
            )
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = contentPadding,
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(12) {
                        RestaurantCardSkeleton(brush = shimmerBrush)
                    }
                }
            }

            isError -> {
                NoInternetState()
            }

            restaurants.isEmpty() -> {
                EmptyState()
            }

            else -> {
                LazyVerticalGrid(
                    state = listState,
                    columns = GridCells.Fixed(columns),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedRestaurants, key = { it.id }) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            isFavorite = restaurant.id in favoriteIds,
                            onClick = onRestaurantClick,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}