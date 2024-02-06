/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
