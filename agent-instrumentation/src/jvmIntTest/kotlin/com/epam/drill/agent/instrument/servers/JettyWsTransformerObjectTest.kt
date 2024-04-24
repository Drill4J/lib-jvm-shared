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
package com.epam.drill.agent.instrument.servers

import java.nio.ByteBuffer
import javax.servlet.ServletContext
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.Session
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import mu.KotlinLogging

class JettyWsTransformerObjectTest : AbstractWsServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit) =
        withWebSocketEndpoint(AnnotatedEndpointConfigurator(), block)

    override fun withWebSocketInterfaceEndpoint(block: (String) -> Unit) =
        withWebSocketEndpoint(InterfaceEndpointConfigurator(), block)

    private fun withWebSocketEndpoint(
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

    class TestRequestJettyServerInterfaceEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(TextMessageHandler(session))
            session.addMessageHandler(BinaryMessageHandler(session))
        }
    }

    private class TextMessageHandler(private val session: Session) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
    }

    private class BinaryMessageHandler(private val session: Session) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            session.basicRemote.sendBinary(ByteBuffer.wrap(attachSessionHeaders(text).encodeToByteArray()))
        }
    }

    private class AnnotatedEndpointConfigurator : WebSocketServerContainerInitializer.Configurator {
        override fun accept(servletContext: ServletContext, serverContainer: ServerContainer) {
            serverContainer.addEndpoint(TestRequestServerAnnotatedEndpoint::class.java)
        }
    }

    private class InterfaceEndpointConfigurator : WebSocketServerContainerInitializer.Configurator {
        override fun accept(servletContext: ServletContext, serverContainer: ServerContainer) {
            val config = ServerEndpointConfig.Builder.create(TestRequestJettyServerInterfaceEndpoint::class.java, "/").build()
            serverContainer.addEndpoint(config)
        }
    }

}
