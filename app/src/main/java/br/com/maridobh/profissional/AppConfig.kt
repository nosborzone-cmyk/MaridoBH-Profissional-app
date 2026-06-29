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

    // Página segura para notificações de suporte/chamado. Evitamos abrir /suporte/
    // porque, em alguns sites WordPress, essa rota não existe e cai no 404.
    val suporteProfissionalUrl: String
        get() = painelProfissionalUrl + "?mbh_app=1&mbh_aba=suporte"

    fun deepLinkToUrl(uri: Uri?): String {
        if (uri == null) return painelProfissionalUrl
        val raw = uri.toString().trim()
        if (raw.isBlank()) return painelProfissionalUrl

        // Links internos HTTPS vindos de notificações ou do próprio WordPress.
        // Antes de abrir, normalizamos rotas de chamado/suporte para não cair no 404.
        if (raw.startsWith(baseUrl) || raw.startsWith(baseUrl.replace("https://", "https://www."))) {
            return normalizeInternalUrl(raw)
        }

        // Deep links nativos do app.
        if (uri.scheme == "maridobh") {
            val host = uri.host.orEmpty().lowercase()
            val id = uri.pathSegments.firstOrNull().orEmpty()
            return when (host) {
                "pedido" -> "$baseUrl/meus-servicos/?mbh_pedido=$id"
                "pedido-cliente" -> "$baseUrl/meus-pedidos/?mbh_pedido=$id"
                "oportunidade" -> "$baseUrl/oportunidades/?pedido_id=$id"
                "chat" -> "$baseUrl/meus-servicos/?mbh_chat=$id"
                "chamado", "ticket", "suporte-resposta" -> suporteUrl(id)
                "suporte", "chamados" -> suporteProfissionalUrl
                "oportunidades" -> "$baseUrl/oportunidades/"
                "perfil" -> "$baseUrl/meu-perfil/"
                "plano" -> "$baseUrl/meu-plano/"
                else -> painelProfissionalUrl
            }
        }

        return painelProfissionalUrl
    }

    private fun normalizeInternalUrl(raw: String): String {
        return try {
            val lower = raw.lowercase()
            val uri = Uri.parse(raw)

            // Qualquer link de suporte/chamado/ticket que vier da push é convertido
            // para uma rota segura dentro do painel do profissional.
            val supportKeywords = listOf("chamado", "chamados", "suporte", "ticket", "ocorrencia", "ocorrência")
            val looksLikeSupport = supportKeywords.any { lower.contains(it) }
            if (looksLikeSupport) {
                val id = firstQuery(uri, "chamado_id", "ticket_id", "suporte_id", "case_id", "id", "mbh_chamado")
                    ?: Regex("#(\\d+)").find(raw)?.groupValues?.getOrNull(1)
                    ?: Regex("(?:chamado|ticket|suporte)[^0-9]{0,12}(\\d+)", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)
                return suporteUrl(id.orEmpty())
            }

            // Se a notificação vier apontando para wp-admin, REST, AJAX ou página vazia,
            // abrimos o painel em vez de exibir erro no app.
            if (lower.contains("/wp-admin") || lower.contains("admin-ajax.php") || lower.contains("/wp-json/")) {
                return painelProfissionalUrl
            }

            raw
        } catch (_: Exception) {
            painelProfissionalUrl
        }
    }

    private fun firstQuery(uri: Uri, vararg keys: String): String? {
        for (key in keys) {
            val value = uri.getQueryParameter(key)
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun suporteUrl(id: String): String {
        val clean = id.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return if (clean.isNotBlank()) {
            "$suporteProfissionalUrl&mbh_chamado=$clean&chamado_id=$clean"
        } else {
            suporteProfissionalUrl
        }
    }

    fun notificationIntentToUrl(intent: Intent?): String {
        if (intent == null) return painelProfissionalUrl

        val extras = intent.extras
        if (extras != null) {
            val map = mutableMapOf<String, String>()
            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value != null) map[key] = value.toString()
            }
            buildNotificationDeepLink(map)?.let { return deepLinkToUrl(Uri.parse(it)) }
        }

        intent.data?.let { return deepLinkToUrl(it) }
        return painelProfissionalUrl
    }

    fun buildNotificationDeepLink(data: Map<String, String>): String? {
        fun first(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            data[key]?.takeIf { it.isNotBlank() }
        }

        val titleBody = listOfNotNull(
            first("title", "notification_title", "titulo"),
            first("body", "message", "mensagem", "notification_body", "texto")
        ).joinToString(" ")

        val direct = first("deep_link", "deeplink", "url", "link", "target_url", "open_url")
        if (!direct.isNullOrBlank() && direct != "OPEN_MARIDOBH") {
            val lowerDirect = direct.lowercase()
            val lowerText = titleBody.lowercase()
            val supportFromText = lowerText.contains("chamado") || lowerText.contains("suporte") || lowerText.contains("ticket")
            if (supportFromText || lowerDirect.contains("chamado") || lowerDirect.contains("suporte") || lowerDirect.contains("ticket")) {
                val id = first("chamado_id", "ticket_id", "suporte_id", "case_id", "mbh_chamado", "entity_id", "resource_id")
                    ?: Regex("#(\\d+)").find(titleBody)?.groupValues?.getOrNull(1)
                    ?: Regex("#(\\d+)").find(direct)?.groupValues?.getOrNull(1)
                return if (!id.isNullOrBlank()) "maridobh://chamado/$id" else "maridobh://suporte"
            }
            return direct
        }

        val pedidoId = first(
            "pedido_id", "order_id", "id_pedido", "servico_id", "service_id",
            "atendimento_id", "mbh_pedido", "mbh_pedido_id", "post_id"
        )
        val chatId = first("chat_id", "conversation_id", "mensagem_pedido_id")
        val chamadoId = first("chamado_id", "ticket_id", "suporte_id", "case_id", "mbh_chamado")
            ?: Regex("#(\\d+)").find(titleBody)?.groupValues?.getOrNull(1)
                ?.takeIf { titleBody.lowercase().contains("chamado") || titleBody.lowercase().contains("suporte") || titleBody.lowercase().contains("ticket") }
        val screen = (first("screen", "type", "event", "action", "notification_type", "target") ?: "").lowercase()
        val entityType = (first("entity", "entity_type", "resource", "resource_type") ?: "").lowercase()
        val entityId = first("entity_id", "resource_id")

        val tipo = (first("tipo", "notification_type", "type") ?: "").lowercase()
        val combined = "$screen $tipo $entityType ${titleBody.lowercase()}"

        return when {
            !chatId.isNullOrBlank() -> "maridobh://chat/$chatId"
            !chamadoId.isNullOrBlank() -> "maridobh://chamado/$chamadoId"
            entityType.contains("chamado") && !entityId.isNullOrBlank() -> "maridobh://chamado/$entityId"
            entityType.contains("suporte") && !entityId.isNullOrBlank() -> "maridobh://chamado/$entityId"
            combined.contains("chamado") || combined.contains("suporte") || combined.contains("ticket") -> "maridobh://suporte"
            (entityType.contains("pedido") || entityType.contains("servico") || entityType.contains("atendimento")) && !entityId.isNullOrBlank() -> "maridobh://pedido/$entityId"
            !pedidoId.isNullOrBlank() && (screen.contains("oportun") || tipo.contains("nova_oportunidade") || tipo.contains("chamariz")) -> "maridobh://oportunidade/$pedidoId"
            !pedidoId.isNullOrBlank() && (screen.contains("cliente") || screen.contains("meus_pedidos")) -> "maridobh://pedido-cliente/$pedidoId"
            !pedidoId.isNullOrBlank() && (screen.contains("chat") || screen.contains("mensagem") || screen.contains("conversa")) -> "maridobh://chat/$pedidoId"
            !pedidoId.isNullOrBlank() -> "maridobh://pedido/$pedidoId"
            screen.contains("oportun") -> "maridobh://oportunidades"
            screen.contains("perfil") -> "maridobh://perfil"
            screen.contains("suporte") || screen.contains("chamado") -> "maridobh://suporte"
            else -> null
        }
    }
}
