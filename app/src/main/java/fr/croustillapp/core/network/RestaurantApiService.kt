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

/**
 * Modèle de données décodant la structure de réponse pour un restaurant unique.
 * Data transfer model wrapping a successful single restaurant API response body payload.
 */
@Serializable
data class SingleRestaurantResponse(
    val success: Boolean,
    val data: fr.croustillapp.features.data.RestaurantDto
)

/**
 * Définition des points de terminaison (endpoints) de l'API pour Retrofit.
 * Retrofit REST endpoints mapping backend routes for the API network stack.
 */
interface RestaurantApiService {
    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants")
    suspend fun getRestaurants(): ApiResponse

    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants/{code}")
    suspend fun getRestaurantById(@Path("code") code: String): SingleRestaurantResponse

    @Headers("Cache-Control: no-cache")
    @GET("v1/restaurants/status/minimal")
    suspend fun getRestaurantsStatus(): RestaurantStatusMinimalResponse

    @Headers("Cache-Control: no-cache")
    @GET("v1/restaurants/{code}/menu")
    suspend fun getMenu(@Path("code") code: String): MenuResponse
}

/**
 * Client HTTP centralisé (Singleton) configuré pour gérer le cache OkHttp et la désérialisation JSON.
 * Centralized HTTP client manager providing an optimized Retrofit instance with aggressive caching policies.
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.croustillant.menu/"
    @Volatile private var instance: RestaurantApiService? = null

    /**
     * Instancie ou récupère le service Retrofit de manière thread-safe.
     * Builds or retrieves the thread-safe instance configuration of the network service.
     */
    fun getService(context: Context): RestaurantApiService {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext

                // Configuration d'une enveloppe de cache de 100 Mo / Allocate 100 MB local cache pool
                val okHttpClient = OkHttpClient.Builder()
                    .cache(Cache(appContext.cacheDir, 100 * 1024 * 1024))
                    .addInterceptor { chain ->
                        var request = chain.request()
                        // Si hors-ligne, on force la lecture du cache local expiré ou non
                        // If offline, rewires header rules to force cache read fallbacks up to 7 days
                        if (!isReallyOnline(appContext)) {
                            request = request.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                                .build()
                        }
                        chain.proceed(request)
                    }
                    .addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        val cacheControl = response.header("Cache-Control")

                        // Force l'injection des en-têtes de cache si le serveur ne les fournit pas
                        // Overrides missing caching headers dynamically across live pipelines
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
                    .addConverterFactory(MyJsonParser.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(RestaurantApiService::class.java)
                    .also { instance = it }
            }
        }
    }

    /**
     * Vérification synchrone et instantanée de la connectivité réseau active.
     * Synchronous shorthand helper confirming immediate hardware network layer attachment availability.
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