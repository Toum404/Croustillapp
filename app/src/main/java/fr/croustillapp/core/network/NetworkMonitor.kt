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
 * FR: Observateur de l'etat de la connexion Internet de l'appareil en temps reel.
 * EN: Real-time monitor tracking the device's internet connectivity status.
 */
class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // FR: Flux exposant l'etat de la connexion. emet true si Internet est disponible, false sinon.
    // EN: Flow exposing the connection state. Emits true if internet is available, false otherwise.
    val isOnline: Flow<Boolean> = callbackFlow {
        // FR: Definition des callbacks systeme pour intercepter les changements d'etat reseau.
        // EN: Definition of system callbacks to intercept network state variations.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }

        // FR: Requête pour cibler uniquement les reseaux disposant d'une capacite d'acces a Internet.
        // EN: Request criteria configured to target exclusively networks providing internet capabilities.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // FR: Verification immediate de l'etat reseau initial au moment de l'abonnement au flux.
        // EN: Immediate check of the initial network status upon subscribing to the stream.
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        // FR: Nettoyage obligatoire pour desenregistrer le callback et eviter les fuites de memoire.
        // EN: Mandatory cleanup block to unregister the callback and prevent memory leaks.
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}