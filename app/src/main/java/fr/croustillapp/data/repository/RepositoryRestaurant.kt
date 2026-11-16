package fr.croustillapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import fr.croustillapp.data.database.RestaurantDao
import fr.croustillapp.data.database.StatusUpdatePartial
import fr.croustillapp.data.network.RestaurantApiService
import fr.croustillapp.data.model.ApiResponse
import fr.croustillapp.data.model.GeoJsonFeatureCollection
import fr.croustillapp.data.model.MyJsonParser
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.data.model.RestaurantEntity
import fr.croustillapp.data.model.toDomain
import fr.croustillapp.data.model.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * FR: Gestionnaire des donnees des restaurants (Cache local + API).
 * EN: Restaurant data manager (Local Cache + API).
 */
class RepositoryRestaurant(
    private val restaurantDao: RestaurantDao,
    private val apiService: RestaurantApiService,
    private val sharedPreferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val LAST_UPDATE_KEY = "last_status_update_timestamp"
        private const val FIVE_MINUTES_IN_MS = 5 * 60 * 1000L
    }

    suspend fun getRestaurantsByIds(ids: List<String>): List<RestaurantEntity> {
        return restaurantDao.getRestaurantsByIds(ids)
    }

    suspend fun getRestaurantsCount(): Int = withContext(ioDispatcher) {
        restaurantDao.getRestaurantsCount()
    }

    suspend fun loadRestaurantsFromAssetJson(context: Context) {
        withContext(ioDispatcher) {
            try {
                val inputStream = context.assets.open("restaurants_default_list.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val response: ApiResponse = MyJsonParser.decodeFromString(jsonString)

                if (response.success) {
                    restaurantDao.insertAll(response.data.map { it.toEntity() })
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun getMenu(restaurantId: String) = withContext(ioDispatcher) {
        apiService.getMenu(restaurantId)
    }

    fun getAllRestaurantEntities(): Flow<List<RestaurantEntity>> {
        return restaurantDao.getAllRestaurants()
    }

    suspend fun loadRegionsGeoJson(context: Context): GeoJsonFeatureCollection? {
        return withContext(ioDispatcher) {
            try {
                val inputStream = context.assets.open("restaurants_default_map.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                MyJsonParser.decodeFromString<GeoJsonFeatureCollection>(jsonString)
            } catch (_: Exception) {
                null
            }
        }
    }

    val allRestaurants: Flow<List<Restaurant>> = restaurantDao.getAllRestaurants()
        .distinctUntilChanged()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun refreshRestaurantsIfPossible(): Boolean = withContext(ioDispatcher) {
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

    suspend fun refreshStatusesIfNeeded(): Boolean = withContext(ioDispatcher) {
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

    suspend fun fetchRestaurantById(restaurantId: String): Restaurant? = withContext(ioDispatcher) {
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