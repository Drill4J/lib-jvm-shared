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
package com.epam.drill.agent.websocket

import java.io.File
import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.ContainerProvider
import javax.websocket.WebSocketContainer
import org.eclipse.jetty.websocket.jsr356.ClientContainer
import mu.KotlinLogging
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.common.agent.configuration.HEADER_AGENT_CONFIG

private const val HEADER_CONTENT_ENCODING = "Content-Encoding"

class WsClientConnector(
    private val uri: URI,
    private val endpoint: WsClientEndpoint
) : ClientEndpointConfig.Configurator() {

    private val logger = KotlinLogging.logger {}
    private val container = ContainerProvider.getWebSocketContainer().apply(::configureWebSocketContainer)
    private val deflateExtension = container.installedExtensions.find { it.name == "permessage-deflate" }
    private val config = ClientEndpointConfig.Builder.create()
        .extensions(listOf(deflateExtension))
        .configurator(this)
        .build()

    override fun beforeRequest(headers: MutableMap<String, MutableList<String>>) {
        WsConfiguration.generateAgentConfigInstanceId()
        headers.put(HEADER_AGENT_CONFIG, mutableListOf(WsConfiguration.getAgentConfigHexString()))
        headers.put(HEADER_CONTENT_ENCODING, mutableListOf("deflate"))
    }

    fun connect() {
        logger.debug { "connect: Connecting to adminUrl=$uri" }
        container.connectToServer(endpoint, config, uri)
        endpoint.getLatch().await()
        logger.debug { "connect: Connected to adminUrl=$uri" }
    }

    fun isContainerRunning() = (container as ClientContainer).isRunning

    private fun configureWebSocketContainer(container: WebSocketContainer): Unit = with(container as ClientContainer) {
        if(uri.scheme != "wss") return
        val sslTruststore = checkSslTruststorePath(WsConfiguration.getSslTruststore())
        val sslTruststorePass = WsConfiguration.getSslTruststorePassword()
        val sslContextFactory = this.client.httpClient.sslContextFactory!!
        sslTruststore.let(String::isEmpty).let(sslContextFactory::setTrustAll)
        sslTruststore.takeIf(String::isNotEmpty)?.let(sslContextFactory::setTrustStorePath)
        sslTruststorePass.takeIf(String::isNotEmpty)?.let(sslContextFactory::setTrustStorePassword)
        logger.debug { "configureWebSocketContainer: SSL configured, trustAll: ${sslContextFactory.isTrustAll}" }
        logger.debug { "configureWebSocketContainer: SSL configured, truststore: ${sslContextFactory.trustStorePath}" }
    }

    private fun checkSslTruststorePath(filePath: String) = File(filePath).run {
        val drillPath = WsConfiguration.getDrillInstallationDir()
            ?.removeSuffix(File.pathSeparator)
            ?.takeIf(String::isNotEmpty)
            ?: "."
        this.takeIf(File::exists)?.let(File::getAbsolutePath)
            ?: this.takeUnless(File::isAbsolute)?.let { File(drillPath).resolve(it).absolutePath }
            ?: filePath
    }

}
