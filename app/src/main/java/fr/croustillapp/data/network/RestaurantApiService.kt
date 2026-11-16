package fr.croustillapp.data.network

import android.content.Context
import android.content.pm.ApplicationInfo
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.croustillapp.R
import fr.croustillapp.data.model.ApiResponse
import fr.croustillapp.data.model.MenuResponse
import fr.croustillapp.data.model.MyJsonParser
import fr.croustillapp.data.model.RestaurantDto
import fr.croustillapp.data.model.RestaurantStatusMinimalResponse
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
    val data: RestaurantDto
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

                val app = appContext.getString(R.string.app_name)
                val nom = appContext.getString(R.string.auteur)
                val github = appContext.getString(R.string.url_github)

                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                val versionName = packageInfo.versionName ?: appContext.getString(R.string.app_version)

                val userAgent = if (isDebugMode(appContext)) {
                    "$app/$versionName (Discord : $nom) (+$github) [(MODE DEVELOPPEMENT) App frontend libre pour Android.]"
                } else {
                    "$app/$versionName (Discord : $nom) (+$github) [App frontend libre pour Android 13 et plus.]"
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
                        val request = chain.request()
                        val response = chain.proceed(request)

                        val cacheControlHeader = request.header("Cache-Control")
                        val isNoCacheRequested = cacheControlHeader?.contains("no-cache") == true

                        if (isNoCacheRequested) {
                            response
                        } else {
                            response.newBuilder()
                                .removeHeader("Pragma")
                                .header("Cache-Control", "public, max-age=604800")
                                .build()
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
}