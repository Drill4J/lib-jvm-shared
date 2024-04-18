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

import java.util.logging.LogManager
import javax.servlet.ServletContextEvent
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint
import javax.websocket.server.ServerEndpointConfig
import org.apache.catalina.servlets.DefaultServlet
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.websocket.server.Constants
import org.apache.tomcat.websocket.server.WsContextListener
import mu.KotlinLogging

class TomcatWsTransformerObjectTest : AbstractWsServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit) =
        withWebSocketEndpoint(AnnotatedEndpointApplicationListener::class.java.name, block)

    override fun withWebSocketInterfaceEndpoint(block: (String) -> Unit) =
        withWebSocketEndpoint(InterfaceEndpointApplicationListener::class.java.name, block)

    private fun withWebSocketEndpoint(applicationListener: String, block: (String) -> Unit)  = Tomcat().run {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"))
            this.setBaseDir("./build")
            this.setPort(0)
            val context = this.addContext("", null)
            this.addServlet(context.path, DefaultServlet::class.simpleName, DefaultServlet())
            context.addServletMappingDecoded("/", DefaultServlet::class.simpleName)
            context.addApplicationListener(applicationListener)
            this.start()
            block("ws://localhost:${connector.localPort}")
        } finally {
            this.stop()
        }
    }

    class AnnotatedEndpointApplicationListener : WsContextListener() {
        override fun contextInitialized(sce: ServletContextEvent) {
            super.contextInitialized(sce)
            val container = sce.servletContext.getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE)
            (container as ServerContainer).addEndpoint(TestRequestAnnotatedEndpoint::class.java)
        }
    }

    class InterfaceEndpointApplicationListener : WsContextListener() {
        override fun contextInitialized(sce: ServletContextEvent) {
            super.contextInitialized(sce)
            val container = sce.servletContext.getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE)
            val config = ServerEndpointConfig.Builder.create(TestRequestInterfaceEndpoint::class.java, "/").build()
            (container as ServerContainer).addEndpoint(config)
        }
    }

    @ServerEndpoint(value = "/")
    class TestRequestAnnotatedEndpoint {
        @OnMessage
        @Suppress("unused")
        fun onMessage(message: String, session: Session) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
    }

    class TestRequestInterfaceEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(String::class.java) { message ->
                session.basicRemote.sendText(attachSessionHeaders(message))
            }
        }
    }

}
