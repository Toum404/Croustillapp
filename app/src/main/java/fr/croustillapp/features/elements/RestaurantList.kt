package fr.croustillapp.features.elements

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import fr.croustillapp.core.components.EmptyState
import fr.croustillapp.core.components.NoInternetState
import fr.croustillapp.core.components.ServerErrorState
import fr.croustillapp.features.data.Restaurant

@Composable
fun RestaurantList(
    restaurants: List<Restaurant>,
    isLoading: Boolean,
    errorType: ErrorType,
    favoriteIds: Set<String>,
    isPrecisionExact: Boolean,
    onRestaurantClick: (Restaurant) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyGridState = rememberLazyGridState()
) {
    val shimmerBrush = rememberShimmerBrush()
    val orientation = LocalConfiguration.current.orientation

    // FR: Adaptation du nombre de colonnes de la grille selon l'orientation de l'écran.
    // EN: Adapts the layout grid column footprint matching current hardware screen orientation configurations.
    val columns = remember(orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
    }

    // FR: Tri mémorisé remontant systématiquement les restaurants favoris en tête de liste.
    // EN: Memoized sorting pattern systematically bubbling favorite restaurants to the top of the stream.
    val displayedRestaurants = remember(restaurants, favoriteIds) {
        restaurants.sortedByDescending { it.id in favoriteIds }
    }

    Box(modifier = modifier) {
        when {
            // FR: État de chargement - Grille fixe non-scollable affichant 10 squelettes animés.
            // EN: Loading state - Fixed non-scrollable layout structure rendering 10 animated skeleton blocks.
            isLoading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = contentPadding,
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(10) {
                        RestaurantCardSkeleton(brush = shimmerBrush)
                    }
                }
            }

            // FR: État d'erreur - Affiche la vue dédiée (Réseau ou Serveur) si la liste locale est vide.
            // EN: Error state - Presents standard error placeholders (No internet or API errors) when local data cache is empty.
            restaurants.isEmpty() && errorType != ErrorType.None -> {
                when (errorType) {
                    ErrorType.NoInternet -> NoInternetState()
                    ErrorType.ServerError -> ServerErrorState()
                    else -> Spacer(modifier = Modifier.fillMaxSize())
                }
            }

            // FR: Liste vide - Déclenché si aucun restaurant ne correspond aux filtres appliqués.
            // EN: Empty fallback - Triggered whenever filtering parameters return an empty local output scope.
            restaurants.isEmpty() -> {
                EmptyState()
            }

            // FR: État nominal - Rendu fluide de la liste des restaurants triée et animée.
            // EN: Nominal state - Seamlessly rendering the fully sorted and animated active restaurant catalog grid.
            else -> {
                LazyVerticalGrid(
                    state = listState,
                    columns = GridCells.Fixed(columns),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = displayedRestaurants,
                        // FR: Clé incluant le statut de favori pour forcer l'animation de déplacement d'item de Compose.
                        // EN: Tracking key embedding favorite states to safely enforce native Compose item repositioning animations.
                        key = { restaurant -> "${restaurant.id}_${restaurant.id in favoriteIds}" }
                    ) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            isFavorite = restaurant.id in favoriteIds,
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