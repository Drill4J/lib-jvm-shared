package com.epam.drill.agent.websocket

import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.ContainerProvider
import mu.KotlinLogging
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.common.agent.configuration.HEADER_AGENT_CONFIG

private const val HEADER_CONTENT_ENCODING = "Content-Encoding"

class WsClientConnector(
    private val uri: URI,
    private val endpoint: WsClientEndpoint
) : ClientEndpointConfig.Configurator() {

    private val logger = KotlinLogging.logger {}
    private val container = ContainerProvider.getWebSocketContainer()
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

}
