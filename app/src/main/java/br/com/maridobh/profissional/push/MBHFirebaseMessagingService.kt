package br.com.maridobh.profissional.push

import br.com.maridobh.profissional.AppConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MBHFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenManager.saveToken(this, token)
        PushTokenManager.markTokenPending(this)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data.toMutableMap()
        val title = message.notification?.title ?: data["title"] ?: "MaridoBH Profissional"
        val body = message.notification?.body ?: data["body"] ?: "Você tem uma nova atualização."
        data["title"] = title
        data["body"] = body
        val deepLink = AppConfig.buildNotificationDeepLink(data)
        NotificationHelper.show(this, title, body, deepLink, data)
    }
}
