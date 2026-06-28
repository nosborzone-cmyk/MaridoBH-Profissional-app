package br.com.maridobh.profissional.work

import android.content.Context
import br.com.maridobh.profissional.mobile.MobileApiClient
import org.json.JSONObject

object WorkSessionManager {
    private const val PREFS = "mbh_work_session"
    private const val KEY_ACTIVE = "active"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_LAST_SUMMARY = "last_summary"

    fun isActive(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    fun startedAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_STARTED_AT, 0L)

    fun start(context: Context) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_STARTED_AT, now)
            .apply()
        sync(context, "started")
    }

    fun stop(context: Context) {
        val durationMs = if (startedAt(context) > 0) System.currentTimeMillis() - startedAt(context) else 0L
        val summary = "Tempo online: ${durationMs / 60000} min"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, false)
            .putString(KEY_LAST_SUMMARY, summary)
            .apply()
        sync(context, "stopped", durationMs)
    }

    fun statusJson(context: Context): JSONObject = JSONObject().apply {
        put("active", isActive(context))
        put("started_at", startedAt(context))
        put("duration_seconds", if (isActive(context) && startedAt(context) > 0) (System.currentTimeMillis() - startedAt(context)) / 1000 else 0)
        put("last_summary", context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_SUMMARY, ""))
    }

    private fun sync(context: Context, event: String, durationMs: Long = 0L) {
        val payload = JSONObject().apply {
            put("platform", "android")
            put("event", event)
            put("started_at", startedAt(context))
            put("duration_seconds", durationMs / 1000)
            put("created_at", System.currentTimeMillis())
        }
        MobileApiClient.syncWorkSession(context, payload)
    }
}
