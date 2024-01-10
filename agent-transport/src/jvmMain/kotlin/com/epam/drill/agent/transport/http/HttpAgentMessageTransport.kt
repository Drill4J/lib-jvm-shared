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
package com.epam.drill.agent.transport.http

import java.io.File
import java.net.URI
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.entity.GzipCompressingEntity
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.ssl.SSLContextBuilder
import com.epam.drill.agent.transport.AgentMessageTransport
import com.epam.drill.common.agent.transport.AgentMessageDestination
import mu.KotlinLogging

class HttpAgentMessageTransport(
    adminAddress: String,
    sslTruststore: String = "",
    sslTruststorePass: String = ""
) : AgentMessageTransport<ByteArray> {

    private val logger = KotlinLogging.logger {}
    private val clientBuilder = HttpClientBuilder.create()
    private val adminUri = URI(adminAddress)
    private val contentTypes = mutableMapOf<String, ContentType>()

    init {
        logger.debug { "configure: Using adminAddress: $adminUri" }
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
                .also(configureHostnameVerifier).build()
            val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory).build()
            clientBuilder.setConnectionManager(connectionManager)
            logger.debug { "constructor: SSL configured, truststore: $sslTruststore" }
            logger.debug { "constructor: SSL configured, trustAll: ${sslTruststore.isEmpty()}" }
        }
    }

    override fun send(
        destination: AgentMessageDestination,
        message: ByteArray,
        contentType: String
    ) = clientBuilder.build().use {
        val request = when (destination.type) {
            "GET" -> HttpGet(adminUri.resolve(destination.target))
            "POST" -> HttpPost(adminUri.resolve(destination.target))
            "PUT" -> HttpPut(adminUri.resolve(destination.target))
            else -> throw IllegalArgumentException("Unknown destination type: ${destination.type}")
        }
        val mimeType = contentType.takeIf(String::isNotEmpty) ?: ContentType.WILDCARD.mimeType
        request.setHeader(HttpHeaders.CONTENT_TYPE, mimeType)
        request.entity = GzipCompressingEntity(ByteArrayEntity(message, getContentType(mimeType)))
        logger.trace { "execute: Request to ${request.uri}, method: ${request.method}, message=$message" }
        HttpResponseStatus(it.execute(request, ::statusResponseHandler)!!)
    }

    private fun statusResponseHandler(response: ClassicHttpResponse) = response.code

    private fun getContentType(mimeType: String) = contentTypes.getOrPut(mimeType) { ContentType.create(mimeType) }

}