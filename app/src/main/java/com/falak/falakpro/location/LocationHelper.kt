package com.falak.falakpro.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class LocationData(
    val latitude: Double = -6.9147,
    val longitude: Double = 107.6098,
    val altitude: Double = 0.0,
    val address: String = "Mencari Lokasi..."
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationData())
    val locationState: StateFlow<LocationData> = _locationState

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // First, check last known locations from all sources (Fused, GPS, Network) to get an instant lock
        getFreshestLastKnownLocation()

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000 // Update every 5 seconds for responsive tracking
            )
            .setMinUpdateIntervalMillis(2000)
            .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val bestLocation = locationResult.lastLocation
                    if (bestLocation != null) {
                        updateLocationWithAddress(bestLocation)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getFreshestLastKnownLocation(): Location? {
        var freshestLocation: Location? = null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        // Check LocationManager GPS
        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    if (freshestLocation == null || loc.time > freshestLocation.time) {
                        freshestLocation = loc
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check LocationManager Network (Cellular / WiFi)
        try {
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    if (freshestLocation == null || loc.time > freshestLocation.time) {
                        freshestLocation = loc
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update if we found any last known location
        freshestLocation?.let {
            updateLocationWithAddress(it)
        }
        return freshestLocation
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation(onComplete: (Boolean) -> Unit = {}) {
        // Instant check of freshest last known location first
        val currentFreshLoc = getFreshestLastKnownLocation()
        if (currentFreshLoc != null && (System.currentTimeMillis() - currentFreshLoc.time) < 60000) { // less than 1 minute old
            onComplete(true)
            return
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var isCompleted = false
        
        val timeoutRunnable = Runnable {
            if (!isCompleted) {
                isCompleted = true
                tryAlternateLocationProviders { success ->
                    if (success) {
                        onComplete(true)
                    } else {
                        fetchLocationByIp(onComplete)
                    }
                }
            }
        }
        
        // Timeout after 3 seconds for FusedLocationProvider, fallback to direct LocationManager or IP
        handler.postDelayed(timeoutRunnable, 3000)

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnCompleteListener { task ->
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isCompleted) {
                        isCompleted = true
                        val location = if (task.isSuccessful) task.result else null
                        if (location != null) {
                            updateLocationWithAddress(location)
                            onComplete(true)
                        } else {
                            tryAlternateLocationProviders { success ->
                                if (success) {
                                    onComplete(true)
                                } else {
                                    fetchLocationByIp(onComplete)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            handler.removeCallbacks(timeoutRunnable)
            if (!isCompleted) {
                isCompleted = true
                tryAlternateLocationProviders { success ->
                    if (success) {
                        onComplete(true)
                    } else {
                        fetchLocationByIp(onComplete)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryAlternateLocationProviders(onComplete: (Boolean) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onComplete(false)
            return
        }

        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!hasNetwork && !hasGps) {
            onComplete(false)
            return
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var isCompleted = false

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                handler.removeCallbacksAndMessages(null)
                if (!isCompleted) {
                    isCompleted = true
                    updateLocationWithAddress(location)
                    locationManager.removeUpdates(this)
                    onComplete(true)
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }

        val timeoutRunnable = Runnable {
            if (!isCompleted) {
                isCompleted = true
                locationManager.removeUpdates(listener)
                // Final fallback to IP
                onComplete(false)
            }
        }

        // Timeout setelah 3 detik untuk active scanning dari LocationManager
        handler.postDelayed(timeoutRunnable, 3000)

        try {
            // Request from BOTH Network (Cellular/Wifi) and GPS providers to guarantee fast response
            if (hasNetwork) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, context.mainLooper)
            }
            if (hasGps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, context.mainLooper)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            handler.removeCallbacks(timeoutRunnable)
            if (!isCompleted) {
                isCompleted = true
                locationManager.removeUpdates(listener)
                onComplete(false)
            }
        }
    }

    fun fetchLocationByIp(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = tryFetchIpLocation("https://freeipapi.com/api/json", "freeipapi")
            if (!success) {
                success = tryFetchIpLocation("https://ipwho.is/", "ipwhois")
            }
            if (!success) {
                success = tryFetchIpLocation("https://ipapi.co/json/", "ipapico")
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    private suspend fun tryFetchIpLocation(urlString: String, type: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val lat: Double
                    val lon: Double
                    val city: String
                    val region: String
                    
                    when (type) {
                        "freeipapi" -> {
                            lat = json.getDouble("latitude")
                            lon = json.getDouble("longitude")
                            city = json.optString("cityName", "Lokasi Terdeteksi")
                            region = json.optString("regionName", "")
                        }
                        "ipwhois" -> {
                            if (json.optBoolean("success", false)) {
                                lat = json.getDouble("latitude")
                                lon = json.getDouble("longitude")
                                city = json.optString("city", "Lokasi Terdeteksi")
                                region = json.optString("region", "")
                            } else {
                                conn.disconnect()
                                return@withContext false
                            }
                        }
                        else -> { // ipapico
                            lat = json.getDouble("latitude")
                            lon = json.getDouble("longitude")
                            city = json.optString("city", "Lokasi Terdeteksi")
                            region = json.optString("region", "")
                        }
                    }
                    
                    val addressName = if (region.isNotEmpty()) "$city, $region" else city
                    withContext(Dispatchers.Main) {
                        _locationState.value = LocationData(
                            latitude = lat,
                            longitude = lon,
                            altitude = 0.0,
                            address = addressName
                        )
                    }
                    conn.disconnect()
                    return@withContext true
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        }
    }

    private fun updateLocationWithAddress(location: Location) {
        val addressName = try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality ?: ""
                val rawLocality = addr.locality ?: addr.subAdminArea ?: "Lokasi Terdeteksi"
                
                val cleanLocality = rawLocality
                    .replace("Kecamatan ", "", ignoreCase = true)
                    .replace("Kabupaten ", "", ignoreCase = true)
                    .replace("Kota ", "", ignoreCase = true)
                    .trim()
                
                if (subLocality.isNotEmpty()) "$subLocality, $cleanLocality" else cleanLocality
            } else {
                "Lokasi GPS Aktif"
            }
        } catch (e: Exception) {
            "Lokasi GPS/Jaringan Aktif"
        }

        _locationState.value = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            address = addressName
        )
    }
}
