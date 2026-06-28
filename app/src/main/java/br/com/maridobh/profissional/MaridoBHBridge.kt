package br.com.maridobh.profissional

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import br.com.maridobh.profissional.diagnostics.AppDiagnostics
import br.com.maridobh.profissional.location.PreciseLocationManager
import br.com.maridobh.profissional.push.PushTokenManager
import br.com.maridobh.profissional.mobile.MobileApiClient
import br.com.maridobh.profissional.sync.OfflineQueueManager
import br.com.maridobh.profissional.work.WorkSessionManager
import org.json.JSONObject

class MaridoBHBridge(private val context: Context) {
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return JSONObject(PushTokenManager.devicePayload(context)).toString()
    }

    @JavascriptInterface
    fun getPushToken(): String {
        return PushTokenManager.getToken(context).orEmpty()
    }

    @JavascriptInterface
    fun getLocationStatus(): String {
        return PreciseLocationManager.statusJson(context).toString()
    }

    @JavascriptInterface
    fun getWorkSessionStatus(): String {
        return WorkSessionManager.statusJson(context).toString()
    }


    @JavascriptInterface
    fun getDiagnostics(): String {
        return AppDiagnostics.diagnosticsJson(context).toString()
    }

    @JavascriptInterface
    fun getPendingSyncCount(): Int {
        return OfflineQueueManager.pendingCount(context)
    }

    @JavascriptInterface
    fun syncPending() {
        (context as? MainActivity)?.performSmartSyncFromBridge() ?: MobileApiClient.flushPending(context)
    }

    @JavascriptInterface
    fun smartSync() {
        (context as? MainActivity)?.performSmartSyncFromBridge() ?: MobileApiClient.flushPending(context)
    }

    @JavascriptInterface
    fun openBatterySettings() {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) { openAppSettings() }
    }

    @JavascriptInterface
    fun startPreciseLocation() {
        (context as? MainActivity)?.runOnUiThread { context.requestPreciseLocationFromBridge() }
    }

    @JavascriptInterface
    fun stopPreciseLocation() {
        (context as? MainActivity)?.runOnUiThread { context.stopPreciseLocationFromBridge() }
    }

    @JavascriptInterface
    fun startWorkSession() {
        (context as? MainActivity)?.runOnUiThread { context.startWorkSessionFromBridge() }
    }

    @JavascriptInterface
    fun stopWorkSession() {
        (context as? MainActivity)?.runOnUiThread { context.stopWorkSessionFromBridge() }
    }

    @JavascriptInterface
    fun markPushTokenPendingSync() {
        PushTokenManager.markTokenPending(context)
    }

    @JavascriptInterface
    fun openAppNotificationSettings() {
        openAppSettings()
    }

    @JavascriptInterface
    fun openLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) { openAppSettings() }
    }

    @JavascriptInterface
    fun log(message: String?) {
        if (BuildConfig.MBH_DEBUG_MODE) android.util.Log.d("MBHBridge", message.orEmpty())
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
