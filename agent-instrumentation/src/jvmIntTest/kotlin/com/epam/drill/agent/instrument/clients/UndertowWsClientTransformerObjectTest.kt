package com.epam.drill.agent.instrument.clients

import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.WebSocketContainer
import io.undertow.websockets.jsr.UndertowContainerProvider

class UndertowWsClientTransformerObjectTest : AbstractWsClientTransformerObjectTest() {

    override fun connectToWebsocketAnnotatedEndpoint(endpoint: String) = TestRequestAnnotatedClientEndpoint().run {
        val session = CustomUndertowContainerProvider().container.connectToServer(this, URI(endpoint))
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(endpoint: String) = TestRequestInterfaceClientEndpoint().run {
        val session = CustomUndertowContainerProvider().container
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(endpoint))
        this to session
    }

    private class CustomUndertowContainerProvider : UndertowContainerProvider() {
        public override fun getContainer(): WebSocketContainer = super.getContainer()
    }

}
