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
import kotlin.test.assertNotNull
import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.server.ServerEndpoint
import org.glassfish.tyrus.client.ClientManager
import mu.KLogger
import com.epam.drill.agent.instrument.TestRequestHolder

@Suppress("FunctionName")
abstract class AbstractWsServerTransformerObjectTest {

    protected abstract val logger: KLogger

    protected companion object {
        fun attachSessionHeaders(message: String) = TestRequestHolder.retrieve()
            ?.let {
                val headers = it.headers
                    .map { (key, value) -> "${key}=${value}" }
                    .joinToString("\n")
                "$message\nsession-headers:\n$headers"
            }
            ?: message
    }

    @Test
    fun `test with empty headers request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testEmptyHeadersRequest)

    @Test
    fun `test with empty headers request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testEmptyHeadersRequest)

    @Test
    fun `test with session headers request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testSessionHeadersRequest)

    @Test
    fun `test with session headers request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testSessionHeadersRequest)

    protected abstract fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit)

    protected abstract fun withWebSocketInterfaceEndpoint(block: (String) -> Unit)

    private fun testEmptyHeadersRequest(address: String) {
        TestRequestHolder.remove()
        val responses = callWebsocketEndpoint(address)
        assertEquals(1, responses.size)
        assertEquals("test-request", responses[0])
    }

    private fun testSessionHeadersRequest(address: String) {
        TestRequestHolder.remove()
        val requestHeaders = mapOf(
            "drill-session-id" to "session-123",
            "drill-header-data" to "test-data"
        )
        val responses = callWebsocketEndpoint(address, requestHeaders)
        assertEquals(1, responses.size)
        val responseBody = responses[0].split("\nsession-headers:\n")[0]
        val responseHeaders = responses[0].split("\nsession-headers:\n").getOrNull(1)
            ?.lines()
            ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        assertEquals("test-request", responseBody)
        assertNotNull(responseHeaders)
        assertEquals("session-123", responseHeaders["drill-session-id"])
        assertEquals("test-data", responseHeaders["drill-header-data"])
    }

    private fun callWebsocketEndpoint(
        endpointAddress: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "test-request"
    ) = ClientManager.createClient().run {
        val endpoint = ClientWebSocketEndpoint()
        val config = ClientEndpointConfig.Builder.create().configurator(ClientWebSocketEndpointConfigurator(headers)).build()
        val session = this.connectToServer(endpoint, config, URI(endpointAddress))
        session.basicRemote.sendText(body)
        Thread.sleep(500)
        session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, CloseReason.CloseCodes.NORMAL_CLOSURE.name))
        endpoint.incomingMessages
    }

    @ServerEndpoint(value = "/")
    class TestRequestServerAnnotatedEndpoint {
        @OnMessage
        @Suppress("unused")
        fun onMessage(message: String, session: Session) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
    }

    class TestRequestServerInterfaceEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(String::class.java) { message ->
                session.basicRemote.sendText(attachSessionHeaders(message))
            }
        }
    }

    private class ClientWebSocketEndpoint : Endpoint() {
        val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) =
            session.addMessageHandler(String::class.java, incomingMessages::add)
    }

    private class ClientWebSocketEndpointConfigurator(
        private val drillHeaders: Map<String, String>
    ) : ClientEndpointConfig.Configurator() {
        override fun beforeRequest(headers: MutableMap<String, MutableList<String>>) =
            drillHeaders.forEach { key, value -> headers[key] = mutableListOf(value) }
    }

}
