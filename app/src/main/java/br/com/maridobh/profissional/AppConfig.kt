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
        if (raw.startsWith(baseUrl.replace("https://", "https://www."))) return raw

        // Deep links nativos do app.
        if (uri.scheme == "maridobh") {
            val host = uri.host.orEmpty()
            val id = uri.pathSegments.firstOrNull().orEmpty()
            return when (host) {
                "pedido" -> "$baseUrl/meus-servicos/?mbh_pedido=$id"
                "pedido-cliente" -> "$baseUrl/meus-pedidos/?mbh_pedido=$id"
                "oportunidade" -> "$baseUrl/oportunidades/?pedido_id=$id"
                "chat" -> "$baseUrl/meus-servicos/?mbh_chat=$id"
                "chamado" -> "$baseUrl/suporte/?mbh_chamado=$id"
                "suporte" -> "$baseUrl/suporte/"
                "oportunidades" -> "$baseUrl/oportunidades/"
                "perfil" -> "$baseUrl/meu-perfil/"
                "plano" -> "$baseUrl/meu-plano/"
                else -> painelProfissionalUrl
            }
        }

        return painelProfissionalUrl
    }

    fun notificationIntentToUrl(intent: Intent?): String {
        if (intent == null) return painelProfissionalUrl
        intent.data?.let { return deepLinkToUrl(it) }

        val extras = intent.extras ?: return painelProfissionalUrl
        val map = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            val value = extras.get(key)
            if (value != null) map[key] = value.toString()
        }
        buildNotificationDeepLink(map)?.let { return deepLinkToUrl(Uri.parse(it)) }
        return painelProfissionalUrl
    }

    fun buildNotificationDeepLink(data: Map<String, String>): String? {
        fun first(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            data[key]?.takeIf { it.isNotBlank() }
        }

        val direct = first("deep_link", "deeplink", "url", "link", "target_url", "open_url")
        if (!direct.isNullOrBlank() && direct != "OPEN_MARIDOBH") return direct

        val pedidoId = first(
            "pedido_id", "order_id", "id_pedido", "servico_id", "service_id",
            "atendimento_id", "mbh_pedido", "mbh_pedido_id", "post_id"
        )
        val chatId = first("chat_id", "conversation_id", "mensagem_pedido_id")
        val chamadoId = first("chamado_id", "ticket_id", "suporte_id", "case_id")
        val screen = (first("screen", "type", "event", "action", "notification_type", "target") ?: "").lowercase()
        val entityType = (first("entity", "entity_type", "resource", "resource_type") ?: "").lowercase()
        val entityId = first("entity_id", "resource_id")

        val tipo = (first("tipo", "notification_type", "type") ?: "").lowercase()

        return when {
            !chatId.isNullOrBlank() -> "maridobh://chat/$chatId"
            !chamadoId.isNullOrBlank() -> "maridobh://chamado/$chamadoId"
            entityType.contains("chamado") && !entityId.isNullOrBlank() -> "maridobh://chamado/$entityId"
            (entityType.contains("pedido") || entityType.contains("servico") || entityType.contains("atendimento")) && !entityId.isNullOrBlank() -> "maridobh://pedido/$entityId"
            !pedidoId.isNullOrBlank() && (screen.contains("oportun") || tipo.contains("nova_oportunidade") || tipo.contains("chamariz")) -> "maridobh://oportunidade/$pedidoId"
            !pedidoId.isNullOrBlank() && (screen.contains("cliente") || screen.contains("meus_pedidos")) -> "maridobh://pedido-cliente/$pedidoId"
            !pedidoId.isNullOrBlank() && (screen.contains("chat") || screen.contains("mensagem") || screen.contains("conversa")) -> "maridobh://chat/$pedidoId"
            !pedidoId.isNullOrBlank() -> "maridobh://pedido/$pedidoId"
            screen.contains("oportun") -> "maridobh://oportunidades"
            screen.contains("perfil") -> "maridobh://perfil"
            screen.contains("suporte") || screen.contains("chamado") -> "maridobh://chamado/${entityId.orEmpty()}".takeIf { !entityId.isNullOrBlank() } ?: "maridobh://suporte"
            else -> null
        }
    }

}
