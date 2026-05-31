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
 * Moniteur d'état réseau en temps réel émettant des notifications de connectivité via un Flow réactif.
 * Real-time network state listener emitting connectivity status updates through a reactive coroutine Flow.
 */
class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Flux froid (Flow) qui émet true si l'appareil dispose d'un accès Internet actif, sinon false.
     * Cold stream Flow emitting true if the active transport configuration resolves online, otherwise false.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Émission de l'état initial immédiat au démarrage de l'écoute / Dispatch initial evaluation instantly
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        // Nettoyage du callback à la fermeture du scope du flux / Unregister system hooks upon subscription cancellation
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged() // Évite les doublons d'émissions identiques / Prevents redundant sequential emission values
}