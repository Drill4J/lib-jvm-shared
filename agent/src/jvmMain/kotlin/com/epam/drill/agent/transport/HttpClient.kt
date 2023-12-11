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
package com.epam.drill.agent.transport

import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File
import java.net.URI
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.entity.GzipCompressingEntity
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.ssl.SSLContextBuilder
import mu.KotlinLogging
import com.epam.drill.common.agent.transport.AgentMessage

object HttpClient {

    private val logger = KotlinLogging.logger {}
    private val clientBuilder = HttpClientBuilder.create();

    private lateinit var adminUri: URI

    fun configure(adminAddress: String, sslTruststore: String, sslTruststorePass: String) {
        adminUri = URI(adminAddress)
        logger.debug { "configure: Using adminAddress: ${adminUri}" }
        if (adminUri.scheme == "https") {
            val configureTrustStore: (SSLContextBuilder) -> Unit = {
                if (sslTruststore.isEmpty()) it.loadTrustMaterial { _, _ -> true }
                else it.loadTrustMaterial(File(sslTruststore), sslTruststorePass.toCharArray())
            }
            val configureHostnameVerifier: (SSLConnectionSocketFactoryBuilder) -> Unit = {
                if (sslTruststore.isEmpty()) it.setHostnameVerifier(NoopHostnameVerifier())
            }
            val sslContext = SSLContextBuilder.create()
                .also(configureTrustStore).build()
            val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext)
                .also(configureHostnameVerifier).build();
            val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory).build()
            clientBuilder.setConnectionManager(connectionManager);
            logger.debug { "configure: SSL configured, truststore: $sslTruststore" }
            logger.debug { "configure: SSL configured, trustAll: ${sslTruststore.isEmpty()}" }
        }
    }

    fun post(path: String, entity: AgentMessage): Int = execute(HttpPost(adminUri.resolve(path)), entity)

    fun put(path: String, entity: AgentMessage): Int = execute(HttpPut(adminUri.resolve(path)), entity)

    fun put(path: String, entity: String): Int = execute(HttpPut(adminUri.resolve(path)), entity)

    private fun execute(request: ClassicHttpRequest, entity: AgentMessage) =
        execute(request, ProtoBuf.encodeToHexString(entity))

    private fun execute(request: ClassicHttpRequest, entity: String) = clientBuilder.build().use {
        request.setHeader("Content-Type", "application/x-protobuf")
        request.entity = GzipCompressingEntity(StringEntity(entity))
        logger.trace { "execute: Request to ${request.uri}, method: ${request.method}, message=$entity" }
        it.execute(request, ::statusResponseHandler)!!
    }

    private fun statusResponseHandler(response: ClassicHttpResponse): Int = response.code

}
