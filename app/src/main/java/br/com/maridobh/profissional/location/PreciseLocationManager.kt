package br.com.maridobh.profissional.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import br.com.maridobh.profissional.mobile.MobileApiClient
import org.json.JSONObject

object PreciseLocationManager {
    private const val PREFS = "mbh_precise_location"
    private const val KEY_ACTIVE = "active"
    private const val KEY_LAST_LAT = "last_lat"
    private const val KEY_LAST_LNG = "last_lng"
    private const val KEY_LAST_ACC = "last_accuracy"
    private const val KEY_LAST_AT = "last_at"

    private var listener: LocationListener? = null

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isActive(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    @SuppressLint("MissingPermission")
    fun start(context: Context): Boolean {
        if (!hasPermission(context)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, true).apply()
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
        listener?.let { runCatching { lm.removeUpdates(it) } }
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { saveAndSync(context, location) }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        runCatching { lm.getLastKnownLocation(provider)?.let { saveAndSync(context, it) } }
        lm.requestLocationUpdates(provider, 60_000L, 25f, listener!!)
        return true
    }

    fun stop(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listener?.let { runCatching { lm.removeUpdates(it) } }
        listener = null
    }

    fun saveAndSync(context: Context, location: Location) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_LAT, location.latitude.toString())
            .putString(KEY_LAST_LNG, location.longitude.toString())
            .putFloat(KEY_LAST_ACC, location.accuracy)
            .putLong(KEY_LAST_AT, System.currentTimeMillis())
            .apply()
        MobileApiClient.syncLocation(context, statusJson(context))
    }

    fun statusJson(context: Context): JSONObject {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAST_LAT, null)
        val lng = prefs.getString(KEY_LAST_LNG, null)
        val acc = prefs.getFloat(KEY_LAST_ACC, -1f)
        val lastAt = prefs.getLong(KEY_LAST_AT, 0L)
        return JSONObject().apply {
            put("platform", "android")
            put("precise_location_active", isActive(context))
            put("latitude", lat)
            put("longitude", lng)
            put("accuracy", if (acc >= 0) acc else JSONObject.NULL)
            put("last_location_at", lastAt)
            put("quality", quality(acc, lastAt))
        }
    }

    private fun quality(acc: Float, lastAt: Long): String {
        if (lastAt <= 0L) return "sem_localizacao"
        val ageMinutes = (System.currentTimeMillis() - lastAt) / 60000
        return when {
            ageMinutes > 180 -> "desatualizada"
            acc in 0f..15f -> "excelente"
            acc in 15f..60f -> "boa"
            else -> "baixa"
        }
    }
}
