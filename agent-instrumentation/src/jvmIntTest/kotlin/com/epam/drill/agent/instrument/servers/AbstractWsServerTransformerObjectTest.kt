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
import java.nio.ByteBuffer
import javax.websocket.ClientEndpointConfig
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
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
    fun `test with empty headers text request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testEmptyHeadersTextRequest)

    @Test
    fun `test with empty headers text request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testEmptyHeadersTextRequest)

    @Test
    fun `test with session headers text request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testSessionHeadersTextRequest)

    @Test
    fun `test with session headers text request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testSessionHeadersTextRequest)

    @Test
    fun `test with empty headers binary request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testEmptyHeadersBinaryRequest)

    @Test
    fun `test with empty headers binary request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testEmptyHeadersBinaryRequest)

    @Test
    fun `test with session headers binary request to annotated endpoint`() =
        withWebSocketAnnotatedEndpoint(::testSessionHeadersBinaryRequest)

    @Test
    fun `test with session headers binary request to interface endpoint`() =
        withWebSocketInterfaceEndpoint(::testSessionHeadersBinaryRequest)

    protected abstract fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit)

    protected abstract fun withWebSocketInterfaceEndpoint(block: (String) -> Unit)

    private fun testEmptyHeadersTextRequest(address: String) = testEmptyHeadersRequest(address, "text")

    private fun testSessionHeadersTextRequest(address: String) = testSessionHeadersRequest(address, "text")

    private fun testEmptyHeadersBinaryRequest(address: String) = testEmptyHeadersRequest(address, "binary")

    private fun testSessionHeadersBinaryRequest(address: String) = testSessionHeadersRequest(address, "binary")

    private fun testEmptyHeadersRequest(address: String, type: String) {
        TestRequestHolder.remove()
        val responses = callWebsocketEndpoint(address, type = type)
        assertEquals(1, responses.size)
        assertEquals("test-request", responses[0])
    }

    private fun testSessionHeadersRequest(address: String, type: String) {
        TestRequestHolder.remove()
        val requestHeaders = mapOf(
            "drill-session-id" to "session-123",
            "drill-header-data" to "test-data"
        )
        val responses = callWebsocketEndpoint(address, requestHeaders, type = type)
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
        body: String = "test-request",
        type: String = "text"
    ) = ClientManager.createClient().run {
        val endpoint = ClientWebSocketEndpoint()
        val config = ClientEndpointConfig.Builder.create().configurator(ClientWebSocketEndpointConfigurator(headers)).build()
        val session = this.connectToServer(endpoint, config, URI(endpointAddress))
        when (type) {
            "text" -> session.basicRemote.sendText(body)
            "binary" -> session.basicRemote.sendBinary(ByteBuffer.wrap(body.encodeToByteArray()))
        }
        Thread.sleep(500)
        session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, CloseReason.CloseCodes.NORMAL_CLOSURE.name))
        endpoint.incomingMessages
    }

    @ServerEndpoint(value = "/")
    @Suppress("unused")
    class TestRequestServerAnnotatedEndpoint {
        @OnMessage
        fun onTextMessage(message: String, session: Session) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer, session: Session) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            session.basicRemote.sendBinary(ByteBuffer.wrap(attachSessionHeaders(text).encodeToByteArray()))
        }
    }

    class TestRequestServerInterfaceEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message ->
                session.basicRemote.sendText(attachSessionHeaders(message))
            }
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                val text = ByteArray(message.limit()).also(message::get).decodeToString()
                session.basicRemote.sendBinary(ByteBuffer.wrap(attachSessionHeaders(text).encodeToByteArray()))
            }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(TextMessageHandler(session))
            session.addMessageHandler(BinaryMessageHandler(session))
        }
    }

    private class TextMessageHandler(private val session: Session) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            session.basicRemote.sendText(attachSessionHeaders(message))
        }
    }

    private class BinaryMessageHandler(private val session: Session) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            session.basicRemote.sendBinary(ByteBuffer.wrap(attachSessionHeaders(text).encodeToByteArray()))
        }
    }

    private class ClientWebSocketEndpoint : Endpoint() {
        val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(String::class.java, incomingMessages::add)
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
            }
        }
    }

    private class ClientWebSocketEndpointConfigurator(
        private val drillHeaders: Map<String, String>
    ) : ClientEndpointConfig.Configurator() {
        override fun beforeRequest(headers: MutableMap<String, MutableList<String>>) =
            drillHeaders.forEach { key, value -> headers[key] = mutableListOf(value) }
    }

}
