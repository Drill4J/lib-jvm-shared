package com.epam.drill.agent.instrument.clients

import java.net.URI
import javax.websocket.ClientEndpointConfig
import org.apache.tomcat.websocket.WsWebSocketContainer

class TomcatWsClientTransformerObjectTest : AbstractWsClientTransformerObjectTest() {

    override fun connectToWebsocketAnnotatedEndpoint(endpoint: String) = TestRequestAnnotatedClientEndpoint().run {
        val session = WsWebSocketContainer().connectToServer(this, URI(endpoint))
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(endpoint: String) = TestRequestInterfaceClientEndpoint().run {
        val session = WsWebSocketContainer()
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(endpoint))
        this to session
    }

}
