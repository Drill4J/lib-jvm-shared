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
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint
import org.apache.catalina.servlets.DefaultServlet
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.websocket.server.Constants
import org.apache.tomcat.websocket.server.WsContextListener
import mu.KotlinLogging

class TomcatWsTransformerObjectTest : AbstractWsServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketServer(block: (String) -> Unit) = Tomcat().run {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"))
            this.setBaseDir("./build")
            this.setPort(0)
            val context = this.addContext("", null)
            this.addServlet(context.path, DefaultServlet::class.simpleName, DefaultServlet())
            context.addServletMappingDecoded("/", DefaultServlet::class.simpleName)
            context.addApplicationListener(TestApplicationListener::class.java.name)
            this.start()
            block("ws://localhost:${connector.localPort}")
        } finally {
            this.stop()
        }
    }

    class TestApplicationListener : WsContextListener() {
        override fun contextInitialized(sce: ServletContextEvent) {
            super.contextInitialized(sce)
            val container = sce.servletContext.getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE)
            (container as ServerContainer).addEndpoint(TestRequestEndpoint::class.java)
        }
    }

    @ServerEndpoint(value = "/")
    class TestRequestEndpoint {
        @OnMessage
        @Suppress("unused")
        fun onMessage(message: String, session: Session) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
    }

}
