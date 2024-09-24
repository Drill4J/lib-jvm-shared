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

import kotlin.test.BeforeTest
import java.net.InetSocketAddress
import javax.net.ssl.HttpsURLConnection
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import mu.KLogger
import mu.KotlinLogging
import com.epam.drill.agent.instrument.TestSslContextFactory

class SSLEngineTransformerObjectTest : AbstractHttpServerTransformerObjectTest() {

    override val logger: KLogger = KotlinLogging.logger {}

    @BeforeTest
    fun configureClientTrustAll() =
        HttpsURLConnection.setDefaultSSLSocketFactory(TestSslContextFactory.trustAllContext().socketFactory)

    override fun withHttpServer(block: (String) -> Unit) = HttpsServer.create().run {
        try {
            this.httpsConfigurator = HttpsConfigurator(TestSslContextFactory.testServerContext())
            this.bind(InetSocketAddress(0), 0)
            this.createContext("/", ::testRequestHandler)
            this.start()
            block("https://localhost:${this.address.port}")
        } finally {
            this.stop(2)
        }
    }

    private fun testRequestHandler(exchange: HttpExchange) {
        val requestBody = exchange.requestBody.readBytes()
        exchange.sendResponseHeaders(200, requestBody.size.toLong())
        exchange.responseBody.write(requestBody)
        exchange.responseBody.close()
    }

}
