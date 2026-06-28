package br.com.maridobh.profissional.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import br.com.maridobh.profissional.MainActivity
import br.com.maridobh.profissional.R

object NotificationHelper {
    private const val CHANNEL_ID = "maridobh_oportunidades"
    private const val CHANNEL_NAME = "MaridoBH Profissional"
    private const val GROUP_DEFAULT = "maridobh_atualizacoes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Oportunidades, mensagens, chamados e status de atendimento"
                enableLights(true)
                lightColor = Color.parseColor("#1677ff")
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, title: String, body: String, deepLink: String?, data: Map<String, String> = emptyMap()) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_MARIDOBH"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("mbh_from_push", true)
            putExtra("deep_link", deepLink.orEmpty())
            if (!deepLink.isNullOrBlank()) setData(Uri.parse(deepLink))
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val groupKey = data["notification_group"]
            ?: data["collapse_key"]
            ?: data["pedido_id"]?.let { "pedido_$it" }
            ?: data["chamado_id"]?.let { "chamado_$it" }
            ?: GROUP_DEFAULT

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_maridobh)
            .setContentTitle(title.ifBlank { "MaridoBH Profissional" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (data["notificacao_id"]?.toIntOrNull())
            ?: (data["pedido_id"]?.let { ("pedido_" + it).hashCode() })
            ?: (data["chamado_id"]?.let { ("chamado_" + it).hashCode() })
            ?: System.currentTimeMillis().toInt()
        manager.notify(notificationId, notification)
    }
}
