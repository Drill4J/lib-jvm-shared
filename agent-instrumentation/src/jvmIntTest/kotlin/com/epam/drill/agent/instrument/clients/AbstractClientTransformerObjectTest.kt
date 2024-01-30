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
package com.epam.drill.agent.instrument.clients

import kotlin.test.assertEquals
import java.net.InetSocketAddress
import org.junit.Test
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

@Suppress("FunctionName")
abstract class AbstractClientTransformerObjectTest {

    @Test
    fun `test with empty headers request`() = withHttpServer {
        val response = callHttpEndpoint(it)
        val responseHeaders = response.first
        val responseBody = response.second
        assertEquals("test-agent", responseHeaders["drill-agent-id"])
        assertEquals("test-admin:8080", responseHeaders["drill-admin-url"])
        assertEquals("test-request", responseBody)
    }

    @Test
    fun `test with session headers request`() = withHttpServer {
        val requestHeaders = mapOf(
            "drill-session-id" to "123",
            "drill-session-data" to "data-123"
        )
        val response = callHttpEndpoint(it, requestHeaders)
        val responseHeaders = response.first
        val responseBody = response.second
        assertEquals("test-agent", responseHeaders["drill-agent-id"])
        assertEquals("test-admin:8080", responseHeaders["drill-admin-url"])
        assertEquals("123", responseHeaders["drill-session-id"])
        assertEquals("data-123", responseHeaders["drill-session-data"])
        assertEquals("test-request", responseBody)
    }

    protected abstract fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        request: String = "test-request"
    ): Pair<Map<String, String>, String>

    private fun withHttpServer(block: (String) -> Unit) = HttpServer.create().run {
        try {
            this.bind(InetSocketAddress(0), 0)
            this.createContext("/", ::testRequestHandler)
            this.start()
            block("http://localhost:${this.address.port}")
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
