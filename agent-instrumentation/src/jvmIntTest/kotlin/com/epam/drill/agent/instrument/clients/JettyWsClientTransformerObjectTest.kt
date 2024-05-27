package com.epam.drill.agent.instrument.clients

import java.net.URI
import javax.websocket.ClientEndpointConfig
import org.eclipse.jetty.websocket.jsr356.ClientContainer

class JettyWsClientTransformerObjectTest : AbstractWsClientTransformerObjectTest() {

    override fun connectToWebsocketAnnotatedEndpoint(endpoint: String) = TestRequestAnnotatedClientEndpoint().run {
        val session = ClientContainer().also(ClientContainer::start).connectToServer(this, URI(endpoint))
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(endpoint: String) = TestRequestInterfaceClientEndpoint().run {
        val session = ClientContainer().also(ClientContainer::start)
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(endpoint))
        this to session
    }

}
