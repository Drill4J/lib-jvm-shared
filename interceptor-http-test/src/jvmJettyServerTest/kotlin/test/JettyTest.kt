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
package test

import TestBase
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.*
import responseMessage
import java.io.*
import java.nio.charset.StandardCharsets.*
import javax.servlet.*
import javax.servlet.http.*


open class JettyTest : TestBase() {

    override fun setupServer() {
        val server = Server()
        val connector = ServerConnector(server)
        connector.port = 0
        server.connectors = arrayOf(connector)
        server.handler = object : AbstractHandler() {
            @Throws(IOException::class, ServletException::class)
            override fun handle(
                target: String,
                baseRequest: Request,
                request: HttpServletRequest?,
                response: HttpServletResponse
            ) {
                baseRequest.isHandled = true
                response.contentType = "text/plain"
                val content = responseMessage
                response.setContentLength(content.length)
                val outputStream = response.outputStream
                val writer = OutputStreamWriter(outputStream, UTF_8)
                writer.write(content)
                writer.flush()
            }
        }
        server.start()

        port = (server.connectors.first() as ServerConnector).localPort
    }

}
