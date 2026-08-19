package fr.croustillapp.core.network

import android.content.Context
import android.content.pm.ApplicationInfo
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.croustillapp.features.data.ApiResponse
import fr.croustillapp.features.data.MenuResponse
import fr.croustillapp.features.data.MyJsonParser
import fr.croustillapp.features.data.RestaurantStatusMinimalResponse
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

    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants/{code}/menu")
    suspend fun getMenu(@Path("code") code: String): MenuResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.croustillant.menu/"
    @Volatile private var instance: RestaurantApiService? = null

    private fun isDebugMode(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun getService(context: Context): RestaurantApiService {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext

                val userAgent = if (isDebugMode(appContext)) {
                    "Croustillapp/2.0.0 (utilisateur66309@gmail.com) (+https://github.com/Toum404/Croustillapp) [(MODE DEBUG) App Android libre affichant resto U et menus, avec gestion des deep links.]"
                } else {
                    "Croustillapp/2.0.0 (utilisateur66309@gmail.com) (+https://github.com/Toum404/Croustillapp) [App Android libre affichant resto U et menus, avec gestion des deep links.]"
                }

                val okHttpClient = OkHttpClient.Builder()
                    .cache(Cache(appContext.cacheDir, 100 * 1024 * 1024))
                    .addInterceptor { chain ->
                        val originalRequest = chain.request()
                        val requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", userAgent)
                            .build()

                        chain.proceed(requestWithUserAgent)
                    }
                    .addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        response.newBuilder()
                            .removeHeader("Pragma")
                            .header("Cache-Control", "public, max-age=604800")
                            .build()
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
}