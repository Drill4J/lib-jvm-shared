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
package com.epam.drill.agent.instrument.tomcat

import java.net.URI
import javax.servlet.ServletContextEvent
import javax.websocket.ClientEndpointConfig
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpointConfig
import org.apache.tomcat.websocket.WsWebSocketContainer
import org.apache.tomcat.websocket.server.Constants
import org.apache.tomcat.websocket.server.WsContextListener
import com.epam.drill.agent.instrument.servers.AbstractWsMessagesTransformerObjectTest

class TomcatWsMessagesTransformerObjectTest : AbstractWsMessagesTransformerObjectTest() {

    override fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit) =
        TomcatWsTestServer.withWebSocketEndpoint(AnnotatedEndpointApplicationListener::class.java, block)

    override fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit) =
        TomcatWsTestServer.withWebSocketEndpoint(InterfaceEndpointApplicationListener::class.java, block)

    override fun connectToWebsocketAnnotatedEndpoint(address: String) = TestRequestClientAnnotatedEndpoint().run {
        val session = WsWebSocketContainer().connectToServer(this, URI(address))
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(address: String) = TestRequestClientInterfaceEndpoint().run {
        val session = WsWebSocketContainer()
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(address))
        this to session
    }

    class AnnotatedEndpointApplicationListener : WsContextListener() {
        override fun contextInitialized(sce: ServletContextEvent) {
            super.contextInitialized(sce)
            val container = sce.servletContext.getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE)
            (container as ServerContainer).addEndpoint(TestRequestServerAnnotatedEndpoint::class.java)
        }
    }

    class InterfaceEndpointApplicationListener : WsContextListener() {
        override fun contextInitialized(sce: ServletContextEvent) {
            super.contextInitialized(sce)
            val container = sce.servletContext.getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE)
            val config = ServerEndpointConfig.Builder.create(TestRequestServerInterfaceEndpoint::class.java, "/").build()
            (container as ServerContainer).addEndpoint(config)
        }
    }

}
