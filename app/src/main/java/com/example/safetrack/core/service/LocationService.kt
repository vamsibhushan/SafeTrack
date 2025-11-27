package com.example.safetrack.core.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateDistanceMeters(5f)
        .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        .setWaitForAccurateLocation(true)
        .build()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(enabled: Boolean): Flow<Location> = callbackFlow {
        if (!enabled) {
            close()
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    try {
                        if (location.accuracy <= 50f) { // Only emit locations with good accuracy
                            trySend(location)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error sending location update")
                    }
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Timber.w("Location is not available")
                }
            }
        }

        try {
            // First try to get last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    if (it.accuracy <= 50f) {
                        trySend(it)
                    }
                }
            }

            // Then request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Timber.e(e, "Failed to request location updates")
                close(e)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting location updates")
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (e: Exception) {
                Timber.e(e, "Error removing location updates")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            val result = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            // If current location request fails, try last known location
            result ?: fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Timber.e(e, "Error getting current location")
            try {
                // Fallback to last known location
                fusedLocationClient.lastLocation.await()
            } catch (e2: Exception) {
                Timber.e(e2, "Error getting last known location")
                null
            }
        }
    }

    fun locationToGeoPoint(location: Location): GeoPoint {
        return GeoPoint(location.latitude, location.longitude)
    }
} 