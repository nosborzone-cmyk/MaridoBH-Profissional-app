package br.com.maridobh.profissional.diagnostics

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.maridobh.profissional.BuildConfig
import br.com.maridobh.profissional.location.PreciseLocationManager
import br.com.maridobh.profissional.push.PushTokenManager
import br.com.maridobh.profissional.sync.OfflineQueueManager
import br.com.maridobh.profissional.work.WorkSessionManager
import org.json.JSONArray
import org.json.JSONObject

object AppDiagnostics {
    fun diagnosticsJson(context: Context): JSONObject {
        val items = JSONArray()
        val network = networkStatus(context)
        val pushOk = PushTokenManager.getToken(context).orEmpty().isNotBlank() && notificationsEnabled(context)
        val gpsPermission = PreciseLocationManager.hasPermission(context)
        val gpsProvider = locationProviderEnabled(context)
        val preciseActive = PreciseLocationManager.isActive(context)
        val queueCount = OfflineQueueManager.pendingCount(context)
        val battery = batteryStatus(context)

        items.put(item("internet", if (network.optBoolean("connected")) "ok" else "critical", "Internet", network.optString("label"), "Ações offline serão sincronizadas quando a conexão voltar."))
        items.put(item("push", if (pushOk) "ok" else "warning", "Push", if (pushOk) "Funcionando" else "Permissão/token pendente", "Sem push ativo, oportunidades e mensagens podem chegar com atraso."))
        items.put(item("gps", if (gpsPermission && gpsProvider) "ok" else "critical", "GPS", if (gpsPermission && gpsProvider) "Pronto" else "Permissão ou GPS desativado", "A localização precisa depende do GPS e da permissão do Android."))
        items.put(item("precise_location", if (preciseActive) "ok" else "warning", "Localização Precisa", if (preciseActive) "Ativa" else "Desativada", "Você continua recebendo oportunidades pelo plano, mas com menor precisão."))
        items.put(item("work_session", if (WorkSessionManager.isActive(context)) "ok" else "info", "Jornada", if (WorkSessionManager.isActive(context)) "Em andamento" else "Não iniciada", "A jornada ajuda a acompanhar sua produtividade."))
        items.put(item("sync", if (queueCount == 0) "ok" else "warning", "Sincronização", if (queueCount == 0) "Sem pendências" else "$queueCount item(ns) pendente(s)", "Itens pendentes serão enviados automaticamente."))
        items.put(item("battery", if (battery.optBoolean("low")) "warning" else "ok", "Bateria", battery.optString("label"), "Com bateria baixa, o app pode reduzir a frequência de localização."))
        items.put(item("bridge", "ok", "Bridge Mobile", "Ativa", "Comunicação entre app e plataforma preparada."))

        return JSONObject().apply {
            put("platform", "android")
            put("app_version", BuildConfig.VERSION_NAME)
            put("checked_at", System.currentTimeMillis())
            put("network", network)
            put("battery", battery)
            put("location", PreciseLocationManager.statusJson(context))
            put("work_session", WorkSessionManager.statusJson(context))
            put("sync", OfflineQueueManager.statusJson(context))
            put("items", items)
            put("overall", overall(items))
        }
    }

    fun humanSummary(context: Context): String {
        val json = diagnosticsJson(context)
        val items = json.optJSONArray("items") ?: JSONArray()
        val lines = mutableListOf<String>()
        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            val icon = when (it.optString("status")) {
                "ok" -> "🟢"
                "warning" -> "🟡"
                "critical" -> "🔴"
                else -> "🔵"
            }
            lines.add("$icon ${it.optString("title")}: ${it.optString("message")}")
        }
        return lines.joinToString("\n")
    }

    fun networkStatus(context: Context): JSONObject {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val label = when {
            !connected -> "Sem internet"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi conectado"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Dados móveis conectados"
            else -> "Conectado"
        }
        return JSONObject().apply {
            put("connected", connected)
            put("label", label)
        }
    }

    fun batteryStatus(context: Context): JSONObject {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val low = pct in 1..20
        return JSONObject().apply {
            put("percent", pct)
            put("low", low)
            put("label", if (pct >= 0) "$pct%" else "Indisponível")
        }
    }

    fun notificationsEnabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun locationProviderEnabled(context: Context): Boolean {
        return try {
            val mode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            mode != Settings.Secure.LOCATION_MODE_OFF
        } catch (_: Exception) { true }
    }

    fun correctionIntent(context: Context, issue: String): Intent {
        return when (issue) {
            "gps" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "push" -> if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else appSettingsIntent(context)
            else -> appSettingsIntent(context)
        }
    }

    private fun appSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
    }

    private fun item(key: String, status: String, title: String, message: String, impact: String): JSONObject = JSONObject().apply {
        put("key", key)
        put("status", status)
        put("title", title)
        put("message", message)
        put("impact", impact)
    }

    private fun overall(items: JSONArray): String {
        var hasCritical = false
        var hasWarning = false
        for (i in 0 until items.length()) {
            when (items.optJSONObject(i)?.optString("status")) {
                "critical" -> hasCritical = true
                "warning" -> hasWarning = true
            }
        }
        return when {
            hasCritical -> "critical"
            hasWarning -> "warning"
            else -> "ok"
        }
    }
}
