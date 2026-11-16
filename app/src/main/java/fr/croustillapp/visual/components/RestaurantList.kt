package fr.croustillapp.visual.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens

@Composable
fun RestaurantList(
    restaurants: List<Restaurant>,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    newRestaurantIds: Set<String>,
    isPrecisionExact: Boolean,
    onRestaurantClick: (Restaurant) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyGridState = rememberLazyGridState(),
) {
    val shimmerBrush = rememberShimmerBrush()

    val displayedRestaurants = remember(restaurants, favoriteIds) {
        restaurants.sortedByDescending { it.id in favoriteIds }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = AppDimens.MaxContentWidth)
        ) {

            val minCardWidth = 160.dp
            val spacing = 16.dp

            val columns = remember(maxWidth) {
                val calculated = ((maxWidth + spacing) / (minCardWidth + spacing)).toInt()
                calculated.coerceIn(AppDimens.CARD_LINE_MIN, AppDimens.CARD_LINE_MAX)
            }

            when {
                isLoading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(10) {
                            RestaurantCardSkeleton(brush = shimmerBrush)
                        }
                    }
                }

                restaurants.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    LazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(
                            items = displayedRestaurants,
                            key = { restaurant ->
                                val isFav = restaurant.id in favoriteIds
                                val isNew = restaurant.id in newRestaurantIds
                                "${restaurant.id}_fav_${isFav}_new_$isNew"
                            }
                        ) { restaurant ->
                            RestaurantCard(
                                restaurant = restaurant,
                                isFavorite = restaurant.id in favoriteIds,
                                isNew = restaurant.id in newRestaurantIds,
                                isPrecisionExact = isPrecisionExact,
                                onClick = onRestaurantClick,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}