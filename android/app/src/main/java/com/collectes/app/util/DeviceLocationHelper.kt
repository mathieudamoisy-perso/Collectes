package com.collectes.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class DeviceLocationHelper(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun readBestLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        val cached = bestCachedLocation()
        if (cached != null && ageMillis(cached) < FRESH_CACHE_MS) {
            return@withContext cached
        }

        val fresh = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            requestSingleUpdate()
        }
        fresh ?: cached
    }

    @SuppressLint("MissingPermission")
    private fun bestCachedLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(): Location? =
        suspendCancellableCoroutine { continuation ->
            val provider = preferredProvider()
            if (provider == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val mainHandler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }

            continuation.invokeOnCancellation {
                mainHandler.post { locationManager.removeUpdates(listener) }
            }

            mainHandler.post {
                runCatching {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

    private fun preferredProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> null
        }
    }

    private fun ageMillis(location: Location): Long =
        System.currentTimeMillis() - location.time

    companion object {
        private const val FRESH_CACHE_MS = 5 * 60 * 1000L
        private const val REQUEST_TIMEOUT_MS = 8_000L
    }
}
