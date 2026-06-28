package br.com.maridobh.profissional.push

import android.content.Context
import android.os.Build
import android.util.Log
import br.com.maridobh.profissional.AppConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

object PushTokenManager {
    private const val PREFS = "mbh_mobile_push"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_PENDING = "token_pending_sync"

    fun init(context: Context) {
        NotificationHelper.ensureChannel(context)
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w("MBHPush", "Firebase não inicializado. Adicione app/google-services.json para ativar FCM real.")
                return
            }
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    saveToken(context, token)
                    markTokenPending(context)
                }
                .addOnFailureListener { error ->
                    Log.w("MBHPush", "Não foi possível obter token FCM: ${error.message}")
                }
        } catch (e: Exception) {
            Log.w("MBHPush", "FCM ainda não configurado: ${e.message}")
        }
    }

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun markTokenPending(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, true)
            .apply()
    }

    fun clearPending(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, false)
            .apply()
    }

    fun devicePayload(context: Context): String {
        val token = getToken(context).orEmpty()
        return """
            {
              "tipo":"android",
              "platform":"android",
              "provider":"fcm",
              "push_provider":"fcm",
              "channel":"${AppConfig.APP_CHANNEL}",
              "app":"MaridoBH Profissional",
              "app_version":"${AppConfig.APP_VERSION}",
              "push_provider":"fcm",
              "push_token":"$token",
              "device_uuid":"android_${android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"}",
              "device_model":"${Build.MANUFACTURER} ${Build.MODEL}",
              "android_version":"${Build.VERSION.RELEASE}",
              "capabilities":{
                "push":${token.isNotBlank()},
                "deep_links":true,
                "webview":true,
                "precise_location_ready":true
              }
            }
        """.trimIndent()
    }
}
