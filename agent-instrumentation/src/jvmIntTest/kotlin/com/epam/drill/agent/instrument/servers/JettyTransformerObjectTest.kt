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

import mu.KotlinLogging
import java.util.logging.LogManager
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyTransformerObjectTest : AbstractServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withHttpServer(block: (String) -> Unit) = Server(0).run {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"))
            val context = ServletContextHandler().apply {
                contextPath = "/"
                val context = ServletContextHandler(null, "/", ServletContextHandler.SESSIONS)
                context.addServlet(ServletHolder(TestRequestServlet()), "/*")
            }
            handler = context
            start()
            block("http://localhost:${this.uri.port}")
        } finally {
            stop()
        }
    }

    private class TestRequestServlet : HttpServlet() {
        override fun doPost(request: HttpServletRequest?, response: HttpServletResponse) {
            val requestBody: ByteArray = request!!.inputStream.readBytes()
            response.status = 200
            response.setContentLength(requestBody.size)
            response.outputStream.write(requestBody)
            response.outputStream.close()
        }
    }

}
