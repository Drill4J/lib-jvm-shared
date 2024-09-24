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

import java.util.logging.LogManager
import org.apache.catalina.servlets.DefaultServlet
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.websocket.server.WsContextListener

object TomcatWsTestServer {

    fun withWebSocketEndpoint(listener: Class<out WsContextListener>, block: (String) -> Unit)  = Tomcat().run {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"))
            this.setBaseDir("./build")
            this.setPort(0)
            val context = this.addContext("", null)
            this.addServlet(context.path, DefaultServlet::class.simpleName, DefaultServlet())
            context.addServletMappingDecoded("/", DefaultServlet::class.simpleName)
            context.addApplicationListener(listener.name)
            this.start()
            block("ws://localhost:${connector.localPort}")
        } finally {
            this.stop()
        }
    }

}
