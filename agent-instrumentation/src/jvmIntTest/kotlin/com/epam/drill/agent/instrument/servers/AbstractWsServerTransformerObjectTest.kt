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

import kotlin.test.Test
import kotlin.test.assertEquals
import java.net.URI
import org.glassfish.tyrus.client.ClientManager
import jakarta.websocket.ClientEndpointConfig
import jakarta.websocket.CloseReason
import jakarta.websocket.Endpoint
import jakarta.websocket.EndpointConfig
import jakarta.websocket.Session
import mu.KLogger
import com.epam.drill.agent.instrument.TestRequestHolder

@Suppress("FunctionName")
abstract class AbstractWsServerTransformerObjectTest {

    protected abstract val logger: KLogger

    @Test
    fun `test with empty headers request`() = withWebSocketServer {
        TestRequestHolder.remove()
        val responses = callWebsocketEndpoint(it)
        assertEquals(1, responses.size)
        assertEquals("test-request", responses[0])
    }

    @Test
    fun `test with session headers request`() = withWebSocketServer {
        TestRequestHolder.remove()
        val requestHeaders = mapOf(
            "drill-session-id" to "session-123",
            "drill-header-data" to "test-data"
        )
        val responses = callWebsocketEndpoint(it, requestHeaders)
        assertEquals(1, responses.size)
        val responseBody = responses[0].split("\nsession-headers:\n")[0]
        val responseHeaders = responses[0].split("\nsession-headers:\n")[1]
            .lines()
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
        assertEquals("test-agent", responseHeaders["drill-agent-id"])
        assertEquals("test-admin:8080", responseHeaders["drill-admin-url"])
        assertEquals("session-123", responseHeaders["drill-session-id"])
        assertEquals("test-data", responseHeaders["drill-header-data"])
        assertEquals("test-request", responseBody)
    }

    protected abstract fun withWebSocketServer(block: (String) -> Unit)

    private fun callWebsocketEndpoint(
        endpointAddress: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "test-request"
    ) = ClientManager.createClient().run {
        val endpoint = WebSocketEndpoint()
        val config = ClientEndpointConfig.Builder.create().configurator(WebSocketEndpointConfigurator(headers)).build()
        val session = this.connectToServer(WebSocketEndpoint(), config, URI(endpointAddress))
        session.basicRemote.sendText(body)
        Thread.sleep(500)
        session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, CloseReason.CloseCodes.NORMAL_CLOSURE.name))
        endpoint.incomingMessages
    }

    private class WebSocketEndpoint : Endpoint() {
        val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) =
            session.addMessageHandler(String::class.java, incomingMessages::add)
    }

    private class WebSocketEndpointConfigurator(
        private val drillHeaders: Map<String, String>
    ) : ClientEndpointConfig.Configurator() {
        override fun beforeRequest(headers: MutableMap<String, MutableList<String>>) =
            drillHeaders.forEach { key, value -> headers[key] = mutableListOf(value) }
    }

}
