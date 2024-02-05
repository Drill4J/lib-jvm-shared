package com.epam.drill.agent.instrument

import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object TestSslContextFactory {

    private const val KEYSTORE_FILE = "server.jks"
    private const val KEYSTORE_PASS = "changeit"

    fun trustAllContext() = SSLContext.getInstance("TLS")
        .apply { this.init(null, arrayOf(TrustAllTrustManager()), null) }

    fun testServerContext() = SSLContext.getInstance("TLS").apply {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(ClassLoader.getSystemResourceAsStream(KEYSTORE_FILE), KEYSTORE_PASS.toCharArray())
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, KEYSTORE_PASS.toCharArray())
        this.init(kmf.keyManagers, null, null)
    }

    private class TrustAllTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

}
