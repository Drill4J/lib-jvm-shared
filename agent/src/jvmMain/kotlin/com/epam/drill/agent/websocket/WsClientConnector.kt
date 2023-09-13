package com.epam.drill.agent.websocket

import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.ContainerProvider
import javax.websocket.WebSocketContainer
import org.eclipse.jetty.websocket.jsr356.ClientContainer
import mu.KotlinLogging
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.common.agent.configuration.HEADER_AGENT_CONFIG

//private const val HEADER_CONTENT_ENCODING = "Content-Encoding"

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
        //headers.put(HEADER_CONTENT_ENCODING, mutableListOf("deflate"))
    }

    fun connect() {
        logger.debug { "connect: Connecting to adminUrl=$uri" }
        container.connectToServer(endpoint, config, uri)
        endpoint.getLatch().await()
        logger.debug { "connect: Connected to adminUrl=$uri" }
    }

    private fun configureWebSocketContainer(container: WebSocketContainer): Unit = with(container as ClientContainer) {
        if(uri.scheme != "wss") return
        val sslTruststore = WsConfiguration.getSslTruststore()
        val sslTruststorePass = WsConfiguration.getSslTruststorePassword()
        val sslContextFactory = this.client.httpClient.sslContextFactory!!
        sslTruststore.let(String::isEmpty).let(sslContextFactory::setTrustAll)
        sslTruststore.takeIf(String::isNotEmpty)?.let(sslContextFactory::setTrustStorePath)
        sslTruststorePass.takeIf(String::isNotEmpty)?.let(sslContextFactory::setTrustStorePassword)
        logger.debug { "configureWebSocketContainer: SSL configured, trustAll: ${sslContextFactory.isTrustAll}" }
        logger.debug { "configureWebSocketContainer: SSL configured, truststore: ${sslContextFactory.trustStorePath}" }
    }

}
