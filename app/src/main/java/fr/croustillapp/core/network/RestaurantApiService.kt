package fr.croustillapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fr.croustillapp.features.data.ApiResponse
import fr.croustillapp.features.data.MenuResponse
import fr.croustillapp.features.data.MyJsonParser
import fr.croustillapp.features.data.RestaurantStatusMinimalResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

@Serializable
data class SingleRestaurantResponse(
    val success: Boolean,
    val data: fr.croustillapp.features.data.RestaurantDto
)

/**
 * FR: Définition des points de terminaison de l'API avec directives de cache HTTP intégrées.
 * EN: API endpoints definition specifying embedded HTTP Cache-Control header constraints.
 */
interface RestaurantApiService {
    // FR: Cache autorisé pendant 7 jours pour la liste globale.
    // EN: Cache permitted for up to 7 days for the global catalog.
    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants")
    suspend fun getRestaurants(): ApiResponse

    // FR: Cache autorisé pendant 7 jours pour un restaurant spécifique (Deep Link / Fiche).
    // EN: Cache permitted for up to 7 days for a specific restaurant item (Deep Link / Details).
    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants/{code}")
    suspend fun getRestaurantById(@Path("code") code: String): SingleRestaurantResponse

    // FR: Pas de cache pour garantir la fraîcheur des statuts d'ouverture en temps réel.
    // EN: Caching bypassed to guarantee real-time accuracy for venue operational statuses.
    @Headers("Cache-Control: no-cache")
    @GET("v1/restaurants/status/minimal")
    suspend fun getRestaurantsStatus(): RestaurantStatusMinimalResponse

    // FR: Pas de cache pour assurer la mise à jour instantanée de la carte/menu.
    // EN: Caching bypassed to ensure instant synchronization of live dynamic menus.
    @Headers("Cache-Control: no-cache")
    @GET("v1/restaurants/{code}/menu")
    suspend fun getMenu(@Path("code") code: String): MenuResponse
}

/**
 * FR: Client HTTP configuré avec une politique de mise en cache locale et résilience hors-ligne.
 * EN: HTTP client configured with a local caching policy and offline resilience workflows.
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.croustillant.menu/"
    @Volatile private var instance: RestaurantApiService? = null

    fun getService(context: Context): RestaurantApiService {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext

                val okHttpClient = OkHttpClient.Builder()
                    // FR: Allocation d'un espace de cache de 100 Mo dans le répertoire interne de l'application.
                    // EN: Allocation of a 100 MB dedicated cache storage window within internal storage.
                    .cache(Cache(appContext.cacheDir, 100 * 1024 * 1024))
                    // FR: Intercepteur d'application : Force l'utilisation du cache existant si l'appareil est hors-ligne.
                    // EN: Application Interceptor: Forces existing cache retrieval workflows if the device is offline.
                    .addInterceptor { chain ->
                        var request = chain.request()
                        if (!isReallyOnline(appContext)) {
                            request = request.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                                .build()
                        }
                        chain.proceed(request)
                    }
                    // FR: Intercepteur réseau : Réécrit les en-têtes du serveur pour activer le cache même si absent par défaut.
                    // EN: Network Interceptor: Rewrites server headers to enable caching when missing by default.
                    .addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        val cacheControl = response.header("Cache-Control")

                        if (cacheControl == null || !cacheControl.contains("no-cache")) {
                            response.newBuilder()
                                .removeHeader("Pragma")
                                .header("Cache-Control", "public, max-age=604800")
                                .build()
                        } else {
                            response
                        }
                    }
                    .build()

                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    // FR: Utilisation du parseur JSON KotlinX Serialization pour décoder les réponses de l'API.
                    // EN: Leveraging KotlinX Serialization JSON engine for parsing raw API responses.
                    .addConverterFactory(MyJsonParser.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(RestaurantApiService::class.java)
                    .also { instance = it }
            }
        }
    }

    /**
     * FR: Vérification synchrone et immédiate des capacités de transport réseau disponibles.
     * EN: Synchronous low-level inspection of operational active network capabilities.
     */
    private fun isReallyOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }
}