package br.com.maridobh.profissional.mobile

import android.content.Context
import android.util.Log
import br.com.maridobh.profissional.AppConfig
import br.com.maridobh.profissional.BuildConfig
import br.com.maridobh.profissional.push.PushTokenManager
import br.com.maridobh.profissional.sync.OfflineQueueManager
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object MobileApiClient {
    private val executor = Executors.newSingleThreadExecutor()

    fun postJsonAsync(context: Context, endpoint: String, payload: JSONObject, type: String = "generic", queueOnFail: Boolean = true, onDone: ((Boolean) -> Unit)? = null) {
        executor.execute {
            val ok = try {
                val url = URL(AppConfig.baseUrl + endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("X-MBH-App", "android-profissional")
                    setRequestProperty("X-MBH-App-Version", BuildConfig.VERSION_NAME)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (e: Exception) {
                Log.w("MBHMobileApi", "Falha ao enviar para $endpoint: ${e.message}")
                false
            }
            if (!ok && queueOnFail) {
                OfflineQueueManager.enqueue(context, type, endpoint, payload)
            }
            onDone?.invoke(ok)
        }
    }

    fun syncDevice(context: Context) {
        try {
            val payload = JSONObject(PushTokenManager.devicePayload(context))
            postJsonAsync(context, BuildConfig.MOBILE_DEVICE_ENDPOINT, payload, type = "device")
        } catch (e: Exception) {
            Log.w("MBHMobileApi", "Payload de dispositivo inválido: ${e.message}")
        }
    }

    fun syncLocation(context: Context, payload: JSONObject) {
        postJsonAsync(context, BuildConfig.MOBILE_LOCATION_ENDPOINT, payload, type = "location")
    }

    fun syncWorkSession(context: Context, payload: JSONObject) {
        postJsonAsync(context, BuildConfig.MOBILE_WORK_SESSION_ENDPOINT, payload, type = "work_session")
    }

    fun flushPending(context: Context, onDone: ((Int) -> Unit)? = null) {
        OfflineQueueManager.flush(context, onDone)
    }
}
