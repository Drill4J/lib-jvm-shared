package com.epam.drill.agent.instrument.jetty

import javax.servlet.ServletContext
import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer

object JettyWsTestServer {

    fun withWebSocketEndpoint(
        configurator: WebSocketServerContainerInitializer.Configurator,
        block: (String) -> Unit
    ) = Server().run {
        try {
            val connector = ServerConnector(this)
            this.connectors = arrayOf(connector)
            val context = ServletContextHandler(null, "/", ServletContextHandler.SESSIONS)
            this.handler = context
            WebSocketServerContainerInitializer.configure(context, configurator)
            this.start()
            block("ws://localhost:${connector.localPort}")
        } finally {
            this.stop()
        }
    }

    class AnnotatedEndpointConfigurator(
        private val endpointClass: Class<out Any>
    ) : WebSocketServerContainerInitializer.Configurator {
        override fun accept(servletContext: ServletContext, serverContainer: ServerContainer) {
            serverContainer.addEndpoint(endpointClass)
        }
    }

    class InterfaceEndpointConfigurator(
        private val endpointClass: Class<out Endpoint>
    ) : WebSocketServerContainerInitializer.Configurator {
        override fun accept(servletContext: ServletContext, serverContainer: ServerContainer) {
            val config = ServerEndpointConfig.Builder.create(endpointClass, "/").build()
            serverContainer.addEndpoint(config)
        }
    }

}
