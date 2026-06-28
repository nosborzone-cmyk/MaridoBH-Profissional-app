package br.com.maridobh.profissional.network

import br.com.maridobh.profissional.AppConfig
import br.com.maridobh.profissional.BuildConfig

object MobileEndpoints {
    val registerDeviceUrl: String
        get() = AppConfig.baseUrl + BuildConfig.MOBILE_DEVICE_ENDPOINT
}
