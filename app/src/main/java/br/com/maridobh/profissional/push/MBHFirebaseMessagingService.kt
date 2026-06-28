package br.com.maridobh.profissional.push

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
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "MaridoBH Profissional"
        val body = message.notification?.body ?: data["body"] ?: "Você tem uma nova atualização."
        val deepLink = data["deep_link"] ?: data["deeplink"] ?: data["url"]
        NotificationHelper.show(this, title, body, deepLink)
    }
}
