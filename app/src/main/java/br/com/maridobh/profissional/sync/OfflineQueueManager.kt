package br.com.maridobh.profissional.sync

import android.content.Context
import android.util.Log
import br.com.maridobh.profissional.mobile.MobileApiClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object OfflineQueueManager {
    private const val PREFS = "mbh_offline_sync"
    private const val KEY_QUEUE = "queue"
    private const val MAX_ITEMS = 200

    data class QueueItem(
        val id: String,
        val type: String,
        val endpoint: String,
        val payload: JSONObject,
        val createdAt: Long,
        val attempts: Int
    )

    @Synchronized
    fun enqueue(context: Context, type: String, endpoint: String, payload: JSONObject) {
        val queue = readQueue(context)
        val item = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", type)
            put("endpoint", endpoint)
            put("payload", payload)
            put("created_at", System.currentTimeMillis())
            put("attempts", 0)
        }
        queue.put(item)
        trimAndSave(context, queue)
    }

    @Synchronized
    fun pendingCount(context: Context): Int = readQueue(context).length()

    @Synchronized
    fun pendingByType(context: Context): JSONObject {
        val counts = JSONObject()
        val queue = readQueue(context)
        for (i in 0 until queue.length()) {
            val type = queue.optJSONObject(i)?.optString("type", "outro") ?: "outro"
            counts.put(type, counts.optInt(type, 0) + 1)
        }
        return counts
    }

    fun flush(context: Context, onDone: ((Int) -> Unit)? = null) {
        val queue = readQueue(context)
        if (queue.length() == 0) {
            onDone?.invoke(0)
            return
        }
        val snapshot = mutableListOf<JSONObject>()
        for (i in 0 until queue.length()) queue.optJSONObject(i)?.let { snapshot.add(it) }
        flushNext(context, snapshot, 0, mutableListOf(), onDone)
    }

    private fun flushNext(
        context: Context,
        items: List<JSONObject>,
        index: Int,
        failed: MutableList<JSONObject>,
        onDone: ((Int) -> Unit)?
    ) {
        if (index >= items.size) {
            saveQueue(context, JSONArray().also { arr -> failed.forEach { arr.put(it) } })
            onDone?.invoke(items.size - failed.size)
            return
        }
        val item = items[index]
        val endpoint = item.optString("endpoint")
        val payload = item.optJSONObject("payload") ?: JSONObject()
        MobileApiClient.postJsonAsync(context, endpoint, payload, queueOnFail = false) { ok ->
            if (!ok) {
                val attempts = item.optInt("attempts", 0) + 1
                item.put("attempts", attempts)
                if (attempts < 8) failed.add(item)
            }
            flushNext(context, items, index + 1, failed, onDone)
        }
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_QUEUE).apply()
    }

    fun statusJson(context: Context): JSONObject = JSONObject().apply {
        put("pending_count", pendingCount(context))
        put("pending_by_type", pendingByType(context))
        put("last_checked_at", System.currentTimeMillis())
    }

    private fun readQueue(context: Context): JSONArray {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_QUEUE, "[]") ?: "[]"
        return runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
    }

    private fun trimAndSave(context: Context, queue: JSONArray) {
        val trimmed = JSONArray()
        val start = (queue.length() - MAX_ITEMS).coerceAtLeast(0)
        for (i in start until queue.length()) queue.optJSONObject(i)?.let { trimmed.put(it) }
        saveQueue(context, trimmed)
    }

    private fun saveQueue(context: Context, queue: JSONArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_QUEUE, queue.toString()).apply()
        Log.d("MBHOfflineQueue", "Pendentes: ${queue.length()}")
    }
}
