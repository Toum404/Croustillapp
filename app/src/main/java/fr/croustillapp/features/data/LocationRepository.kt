package fr.croustillapp.features.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FR: Gere la recuperation de la geolocalisation de l'appareil et l'ecoute des changements de providers GPS.
 * EN: Manages device geolocation retrieval and listening for GPS provider state changes.
 */
class LocationRepository(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * FR: Verifie si au moins un fournisseur de localisation (GPS ou Reseau) est active sur l'appareil.
     * EN: Checks if at least one location provider (GPS or Network) is enabled on the device.
     */
    fun isGpsProviderEnabled(): Boolean {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * FR: Verifie si l'application possede les permissions de localisation (Fine ou Coarse).
     * EN: Checks if the application has location permissions granted (Fine or Coarse).
     */
    fun hasLocationPermissions(): Boolean {
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasCoarse || hasFine
    }

    /**
     * FR: Verifie specifiquement si la permission de localisation precise (Fine) est accordee.
     * EN: Specifically checks if fine location permission is granted.
     */
    fun isFineLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * FR: Recupere la position actuelle (utilise d'abord le cache recent < 3 min, puis ecoute les mises a jour en direct).
     * EN: Fetches current location (uses recent cache < 3 min first, then listens for live location updates).
     */
    suspend fun fetchCurrentLocation(): Pair<Double, Double>? {
        if (!hasLocationPermissions() || !isGpsProviderEnabled()) return null

        val provider = when {
            isFineLocationGranted() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            // Verification du cache recent (< 3 minutes) / Check recent cache (< 3 minutes)
            val lastKnownLocation = locationManager.getLastKnownLocation(provider)
            if (lastKnownLocation != null) {
                val ageDuCache = System.currentTimeMillis() - lastKnownLocation.time
                if (ageDuCache < 3 * 60 * 1000) {
                    return Pair(lastKnownLocation.latitude, lastKnownLocation.longitude)
                }
            }

            // Ecoute de la position en direct via Coroutine / Listen to live location via Coroutine
            return suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        if (continuation.isActive) {
                            try {
                                locationManager.removeUpdates(this)
                            } catch (_: SecurityException) {}
                            continuation.resume(Pair(loc.latitude, loc.longitude))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (_: SecurityException) {}
                }

                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        context.mainLooper
                    )
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } catch (_: SecurityException) {
            return null
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * FR: Enregistre un BroadcastReceiver pour ecouter les modifications des services de localisation du systeme.
     * EN: Registers a BroadcastReceiver to listen for changes in system location services.
     */
    fun registerProviderReceiver(onProvidersChanged: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    onProvidersChanged()
                }
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        return receiver
    }

    /**
     * FR: Desenregistre proprement le BroadcastReceiver de localisation.
     * EN: Safely unregisters the location BroadcastReceiver.
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}