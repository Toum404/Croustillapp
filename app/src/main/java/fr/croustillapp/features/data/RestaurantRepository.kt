package fr.croustillapp.features.data

import android.content.SharedPreferences
import androidx.core.content.edit
import fr.croustillapp.core.database.RestaurantDao
import fr.croustillapp.core.database.StatusUpdatePartial
import fr.croustillapp.core.network.RestaurantApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * FR: Gestionnaire des donnees des restaurants (Cache local + API).
 * EN: Restaurant data manager (Local Cache + API).
 */
class RestaurantRepository(
    private val restaurantDao: RestaurantDao,
    private val apiService: RestaurantApiService,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val LAST_UPDATE_KEY = "last_status_update_timestamp"
        private const val FIVE_MINUTES_IN_MS = 5 * 60 * 1000L
    }

    /**
     * FR: Flux reactif exposant la liste des restaurants depuis la base de donnees locale (convertie en objets Domain).
     * EN: Reactive stream exposing the list of restaurants from the local database (mapped to Domain objects).
     */
    val allRestaurants: Flow<List<Restaurant>> = restaurantDao.getAllRestaurants()
        .distinctUntilChanged()
        .map { entities -> entities.map { it.toDomain() } }

    /**
     * FR: Recupere le nombre total de restaurants en cache (Dispatcher IO).
     * EN: Retrieves the total count of cached restaurants (IO Dispatcher).
     */
    suspend fun getRestaurantsCount(): Int = withContext(Dispatchers.IO) {
        restaurantDao.getRestaurantsCount()
    }

    /**
     * FR: Tente de rafraichir la liste complete des restaurants depuis l'API distante et de mettre a jour le cache local.
     * EN: Attempts to refresh the full restaurant list from the remote API and update the local cache.
     */
    suspend fun refreshRestaurantsIfPossible(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRestaurants()
            if (response.success) {
                restaurantDao.insertAll(response.data.map { it.toEntity() })
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * FR: Rafraichit uniquement les statuts d'ouverture si le dernier rafraichissement date de plus de 5 minutes.
     * EN: Refreshes only opening statuses if the last refresh was more than 5 minutes ago.
     */
    suspend fun refreshStatusesIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val lastUpdate = sharedPreferences.getLong(LAST_UPDATE_KEY, 0L)

        if (currentTime - lastUpdate < FIVE_MINUTES_IN_MS) return@withContext false

        try {
            val statusResponse = apiService.getRestaurantsStatus()
            if (statusResponse.success) {
                val updates = statusResponse.data.map { dto ->
                    StatusUpdatePartial(id = dto.code.toString(), isOpen = dto.ouvert)
                }
                restaurantDao.updateAllStatuses(updates)
                sharedPreferences.edit { putLong(LAST_UPDATE_KEY, currentTime) }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * FR: Recupere un restaurant specifique par son identifiant unique depuis l'API distante.
     * EN: Fetches a specific restaurant by its unique identifier from the remote API.
     */
    suspend fun fetchRestaurantById(restaurantId: String): Restaurant? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRestaurantById(restaurantId)
            if (response.success) {
                response.data.toEntity().toDomain()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}