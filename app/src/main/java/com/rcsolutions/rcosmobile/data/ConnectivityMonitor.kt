package com.rcsolutions.rcosmobile.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 2 — real connectivity state.
 *
 * Reports what the device's network actually is, via
 * ConnectivityManager — never a fixed "ONLINE" label. The app keeps
 * working offline from the Firestore disk cache (explicitly enabled
 * in [RcosRepository]); this monitor only tells the UI and the
 * ViewModel whether the live server is reachable right now, so an
 * offline phone shows a real offline state instead of pretending.
 */
enum class ConnectionState {
    UNKNOWN,
    ONLINE,
    OFFLINE,
}

class ConnectivityMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _state.value = ConnectionState.ONLINE
        }

        override fun onLost(network: Network) {
            // Another network may still be up — re-check instead of
            // assuming the device is offline.
            _state.value = if (hasInternetNetwork()) ConnectionState.ONLINE else ConnectionState.OFFLINE
        }

        override fun onUnavailable() {
            _state.value = ConnectionState.OFFLINE
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun hasInternetNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initialState(): ConnectionState = try {
        if (hasInternetNetwork()) ConnectionState.ONLINE else ConnectionState.OFFLINE
    } catch (_: Exception) {
        ConnectionState.UNKNOWN
    }

    fun close() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
