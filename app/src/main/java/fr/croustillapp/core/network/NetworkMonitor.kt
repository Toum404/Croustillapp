package fr.croustillapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * FR: Observateur de l'état de la connexion Internet de l'appareil en temps réel.
 * EN: Real-time monitor tracking the device's internet connectivity status.
 */
class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // FR: Flux exposant l'état de la connexion. Émet true si Internet est disponible, false sinon.
    // EN: Flow exposing the connection state. Emits true if internet is available, false otherwise.
    val isOnline: Flow<Boolean> = callbackFlow {
        // FR: Définition des callbacks système pour intercepter les changements d'état réseau.
        // EN: Definition of system callbacks to intercept network state variations.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }

        // FR: Requête pour cibler uniquement les réseaux disposant d'une capacité d'accès à Internet.
        // EN: Request criteria configured to target exclusively networks providing internet capabilities.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // FR: Vérification immédiate de l'état réseau initial au moment de l'abonnement au flux.
        // EN: Immediate check of the initial network status upon subscribing to the stream.
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        // FR: Nettoyage obligatoire pour désenregistrer le callback et éviter les fuites de mémoire.
        // EN: Mandatory cleanup block to unregister the callback and prevent memory leaks.
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}