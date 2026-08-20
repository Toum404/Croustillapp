package fr.croustillapp.features.data

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * FR: Verifie si la localisation globale du telephone est activee.
     * EN: Checks whether location services are globally enabled on the device.
     */
    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isLocationEnabled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * FR: Verifie si l'application dispose d'une permission de localisation.
     * EN: Checks whether the app has either coarse or fine location permission.
     */
    fun hasLocationPermissions(): Boolean {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasCoarse || hasFine
    }

    /**
     * FR: Indique si la permission de localisation precise est accordee.
     * EN: Indicates whether precise location permission is granted.
     */
    fun isFineLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * FR: On privilegie FUSED_PROVIDER lorsqu'il est disponible.
     * EN: FUSED_PROVIDER is preferred when available.
     */
    suspend fun fetchCurrentLocation(): Pair<Double, Double>? {
        // FR: Sans permission ou si la localisation est desactivee, inutile
        //      de lancer une requête.
        // EN: Without permission or with location disabled, there is no reason
        //     to start a location request.
        if (!hasLocationPermissions() || !isLocationEnabled()) {
            return null
        }

        /**
         * FR: Ne pas deduire le provider à partir de Fine/Coarse.
         * EN: Do not select the provider based on Fine/Coarse.
         */
        val provider = when {
            locationManager.hasProvider(LocationManager.FUSED_PROVIDER) &&
                    locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
                LocationManager.FUSED_PROVIDER

            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER

            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER

            else ->
                return null
        }

        return try {
            val lastKnownLocation =
                locationManager.getLastKnownLocation(provider)

            if (lastKnownLocation != null) {
                val age = System.currentTimeMillis() - lastKnownLocation.time

                if (age in 0..(3 * 60 * 1000L)) {
                    return Pair(
                        lastKnownLocation.latitude,
                        lastKnownLocation.longitude
                    )
                }
            }

            /**
             * FR: Pas de position recente: demander une position actuelle.
             * EN: No recent cached location: request a fresh current location.
             */
            suspendCancellableCoroutine { continuation ->

                val executor = ContextCompat.getMainExecutor(context)
                
                val cancellationSignal = CancellationSignal()

                locationManager.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    executor
                ) { location ->

                    if (continuation.isActive) {
                        continuation.resume(
                            location?.let {
                                Pair(
                                    it.latitude,
                                    it.longitude
                                )
                            }
                        )
                    }
                }

                /**
                 * FR: Si le ViewModel annule la coroutine, on annule la requête de localisation.
                 * EN: If the ViewModel cancels the coroutine, also cancel the location request.
                 */
                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }
            }

        } catch (_: SecurityException) {
            // FR: Permission retiree ou refusee pendant la requête.
            // EN: Permission was revoked or denied while requesting the location.
            null

        } catch (_: Exception) {
            // FR: echec de la recuperation de la position.
            // EN: Failed to retrieve the location.
            null
        }
    }

    /**
     * FR: Observe les changements d'etat des providers de localisation.
     * EN: Observes changes to location provider availability.
     */
    fun registerProviderReceiver(
        onProvidersChanged: () -> Unit
    ): BroadcastReceiver {

        val receiver = object: BroadcastReceiver() {

            override fun onReceive(
                ctx: Context,
                intent: Intent
            ) {
                if (intent.action ==
                    LocationManager.PROVIDERS_CHANGED_ACTION
                ) {
                    onProvidersChanged()
                }
            }
        }

        val filter = IntentFilter(
            LocationManager.PROVIDERS_CHANGED_ACTION
        )

        context.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        return receiver
    }

    /**
     * FR: Desenregistre le receiver lorsqu'il n'est plus necessaire.
     * EN: Unregisters the receiver when it is no longer needed.
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
    }
}