package fr.croustillapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import fr.croustillapp.data.ApiResponse
import fr.croustillapp.data.MenuResponse
import fr.croustillapp.data.MyJsonParser
import fr.croustillapp.data.RestaurantStatusMinimalResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.croustillapp.data.SingleRestaurantResponse
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface RestaurantApiService {
    @Headers("Cache-Control: public, max-age=604800")
    @GET("v1/restaurants")
    suspend fun getRestaurants(): ApiResponse

    // Ta nouvelle route :
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
object RetrofitClient {
    private const val BASE_URL = "https://api.croustillant.menu/"
    @Volatile private var instance: RestaurantApiService? = null

    fun getService(context: Context): RestaurantApiService {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext

                val okHttpClient = OkHttpClient.Builder()
                    .cache(Cache(appContext.cacheDir, 100 * 1024 * 1024))

                    .addInterceptor { chain ->
                        var request = chain.request()
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

    private fun isReallyOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }
}