package br.com.maridobh.profissional

object AppConfig {
    const val APP_CHANNEL = "android_profissional"
    const val APP_VERSION = BuildConfig.VERSION_NAME

    val baseUrl: String
        get() = BuildConfig.SERVER_URL.trimEnd('/')

    val painelProfissionalUrl: String
        get() = baseUrl + BuildConfig.PROFESSIONAL_PATH

    fun deepLinkToUrl(uri: android.net.Uri?): String {
        if (uri == null || uri.scheme != "maridobh") return painelProfissionalUrl
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
}
