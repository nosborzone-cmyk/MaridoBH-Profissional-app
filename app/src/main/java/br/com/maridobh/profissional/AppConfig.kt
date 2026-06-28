package br.com.maridobh.profissional

import android.content.Intent
import android.net.Uri

object AppConfig {
    const val APP_CHANNEL = "android_profissional"
    const val APP_VERSION = BuildConfig.VERSION_NAME

    val baseUrl: String
        get() = BuildConfig.SERVER_URL.trimEnd('/')

    val painelProfissionalUrl: String
        get() = baseUrl + BuildConfig.PROFESSIONAL_PATH

    fun deepLinkToUrl(uri: Uri?): String {
        if (uri == null) return painelProfissionalUrl
        val raw = uri.toString()

        // Links internos HTTPS vindos de notificações ou do próprio WordPress.
        if (raw.startsWith(baseUrl)) return raw

        // Deep links nativos do app.
        if (uri.scheme == "maridobh") {
            val host = uri.host.orEmpty()
            val id = uri.pathSegments.firstOrNull().orEmpty()
            return when (host) {
                "pedido" -> "$baseUrl/meus-servicos/?mbh_pedido=$id"
                "chat" -> "$baseUrl/meus-servicos/?mbh_chat=$id"
                "chamado" -> "$baseUrl/suporte/?mbh_chamado=$id"
                "oportunidades" -> "$baseUrl/oportunidades/"
                "perfil" -> "$baseUrl/meu-perfil/"
                else -> painelProfissionalUrl
            }
        }

        return painelProfissionalUrl
    }

    fun notificationIntentToUrl(intent: Intent?): String {
        if (intent == null) return painelProfissionalUrl
        intent.data?.let { return deepLinkToUrl(it) }

        val extras = intent.extras ?: return painelProfissionalUrl
        val direct = listOf("deep_link", "deeplink", "url", "link", "target_url")
            .firstNotNullOfOrNull { key -> extras.getString(key)?.takeIf { it.isNotBlank() } }
        if (!direct.isNullOrBlank()) return deepLinkToUrl(Uri.parse(direct))

        val pedidoId = extras.getString("pedido_id") ?: extras.getString("order_id") ?: extras.getString("id_pedido")
        val chatId = extras.getString("chat_id")
        val chamadoId = extras.getString("chamado_id") ?: extras.getString("ticket_id")
        val screen = (extras.getString("screen") ?: extras.getString("type") ?: "").lowercase()

        return when {
            !chatId.isNullOrBlank() -> "$baseUrl/meus-servicos/?mbh_chat=$chatId"
            !chamadoId.isNullOrBlank() -> "$baseUrl/suporte/?mbh_chamado=$chamadoId"
            !pedidoId.isNullOrBlank() && (screen.contains("chat") || screen.contains("mensagem")) -> "$baseUrl/meus-servicos/?mbh_chat=$pedidoId"
            !pedidoId.isNullOrBlank() -> "$baseUrl/meus-servicos/?mbh_pedido=$pedidoId"
            screen.contains("oportun") -> "$baseUrl/oportunidades/"
            screen.contains("perfil") -> "$baseUrl/meu-perfil/"
            screen.contains("suporte") || screen.contains("chamado") -> "$baseUrl/suporte/"
            else -> painelProfissionalUrl
        }
    }

    fun buildNotificationDeepLink(data: Map<String, String>): String? {
        val direct = data["deep_link"] ?: data["deeplink"] ?: data["url"] ?: data["link"] ?: data["target_url"]
        if (!direct.isNullOrBlank()) return direct

        val pedidoId = data["pedido_id"] ?: data["order_id"] ?: data["id_pedido"]
        val chatId = data["chat_id"]
        val chamadoId = data["chamado_id"] ?: data["ticket_id"]
        val screen = (data["screen"] ?: data["type"] ?: "").lowercase()

        return when {
            !chatId.isNullOrBlank() -> "maridobh://chat/$chatId"
            !chamadoId.isNullOrBlank() -> "maridobh://chamado/$chamadoId"
            !pedidoId.isNullOrBlank() && (screen.contains("chat") || screen.contains("mensagem")) -> "maridobh://chat/$pedidoId"
            !pedidoId.isNullOrBlank() -> "maridobh://pedido/$pedidoId"
            screen.contains("oportun") -> "maridobh://oportunidades"
            screen.contains("perfil") -> "maridobh://perfil"
            else -> null
        }
    }
}
