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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.nio.ByteBuffer
import java.util.logging.LogManager
import javax.websocket.ClientEndpoint
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.HandshakeResponse
import javax.websocket.MessageHandler
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpoint
import javax.websocket.server.ServerEndpointConfig
import org.glassfish.tyrus.core.TyrusServerEndpointConfigurator
import org.glassfish.tyrus.server.Server
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.common.request.DrillRequest

@Suppress("FunctionName")
abstract class AbstractWsClientTransformerObjectTest {

    @Test
    fun `test annotated endpoint text request-response with empty thread session data`() =
        testEmptySessionDataRequest(::connectToWebsocketAnnotatedEndpoint, "text")

    @Test
    fun `test interface endpoint text request-response with empty thread session data`() =
        testEmptySessionDataRequest(::connectToWebsocketInterfaceEndpoint, "text")

    @Test
    fun `test annotated endpoint text request with existing thread session data`() =
        testExistingSessionDataRequest(::connectToWebsocketAnnotatedEndpoint, "text")

    @Test
    fun `test interface endpoint text request with existing thread session data`() =
        testExistingSessionDataRequest(::connectToWebsocketInterfaceEndpoint, "text")

    @Test
    fun `test annotated endpoint binary request-response with empty thread session data`() =
        testEmptySessionDataRequest(::connectToWebsocketAnnotatedEndpoint, "binary")

    @Test
    fun `test interface endpoint binary request-response with empty thread session data`() =
        testEmptySessionDataRequest(::connectToWebsocketInterfaceEndpoint, "binary")

    @Test
    fun `test annotated endpoint binary request with existing thread session data`() =
        testExistingSessionDataRequest(::connectToWebsocketAnnotatedEndpoint, "binary")

    @Test
    fun `test interface endpoint binary request with existing thread session data`() =
        testExistingSessionDataRequest(::connectToWebsocketInterfaceEndpoint, "binary")

    protected abstract fun connectToWebsocketAnnotatedEndpoint(endpoint: String): Pair<TestRequestClientEndpoint, Session>

    protected abstract fun connectToWebsocketInterfaceEndpoint(endpoint: String): Pair<TestRequestClientEndpoint, Session>

    private fun testEmptySessionDataRequest(
        connect: (String) -> Pair<TestRequestClientEndpoint, Session>,
        type: String
    ) = withWebSocketServer {
        TestRequestHolder.remove()
        val call = callWebSocketEndpoint(it, connect, type = type)
        val incomingMessages = call.first
        val handshakeHeaders = call.second
        assertNull(TestRequestHolder.retrieve())
        assertNull(handshakeHeaders["drill-session-id"])
        assertNull(handshakeHeaders["drill-header-data"])
        assertEquals(2, incomingMessages.size)
        assertEquals("test-request", incomingMessages[1])
    }

    private fun testExistingSessionDataRequest(
        connect: (String) -> Pair<TestRequestClientEndpoint, Session>,
        type: String
    ) = withWebSocketServer {
        TestRequestHolder.store(DrillRequest("session-123", mapOf("drill-header-data" to "test-data")))
        val call = callWebSocketEndpoint(it, connect, type = type)
        val incomingMessages = call.first
        val handshakeHeaders = call.second
        assertEquals("session-123", handshakeHeaders["drill-session-id"])
        assertEquals("test-data", handshakeHeaders["drill-header-data"])
        assertEquals(2, incomingMessages.size)
        assertEquals("test-request", incomingMessages[1])
    }

    private fun callWebSocketEndpoint(
        endpoint: String,
        connect: (String) -> Pair<TestRequestClientEndpoint, Session>,
        body: String = "test-request",
        type: String = "text"
    ) = connect(endpoint).run {
        val incomingMessages = this.first.incomingMessages
        val session = this.second
        when(type) {
            "text" -> session.basicRemote.sendText(body)
            "binary" -> session.basicRemote.sendBinary(ByteBuffer.wrap(body.encodeToByteArray()))
        }
        Thread.sleep(500)
        session.close()
        val handshakeHeaders = incomingMessages[0].removePrefix("session-headers:\n").lines()
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
        incomingMessages to handshakeHeaders
    }

    private fun withWebSocketServer(block: (String) -> Unit) = Server(TestRequestServerEndpoint::class.java).run {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"))
            this.start()
            block("ws://localhost:${this.port}")
        } finally {
            this.stop()
        }
    }

    @Suppress("unused")
    @ServerEndpoint(value = "/", configurator = TestRequestConfigurator::class)
    class TestRequestServerEndpoint {
        @OnOpen
        fun onOpen(session: Session, config: EndpointConfig) = config
            .let { it as ServerEndpointConfig }
            .let { it.configurator as TestRequestConfigurator }
            .headers
            .map { (key, value) -> "${key}=${value}" }
            .joinToString("\n", "session-headers:\n")
            .also(session.basicRemote::sendText)
        @OnMessage
        fun onTextMessage(message: String, session: Session) = session.basicRemote.sendText(message)
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer, session: Session) = session.basicRemote.sendBinary(message)
    }

    class TestRequestConfigurator : TyrusServerEndpointConfigurator() {
        val headers = mutableMapOf<String, String>()
        override fun modifyHandshake(sec: ServerEndpointConfig, req: HandshakeRequest, resp: HandshakeResponse) =
            req.headers.forEach { headers[it.key] = it.value.joinToString(",") }
    }

    @Suppress("unused")
    @ClientEndpoint
    protected class TestRequestAnnotatedClientEndpoint : TestRequestClientEndpoint {
        override val incomingMessages = mutableListOf<String>()
        @OnMessage
        fun onTextMessage(message: String) {
            incomingMessages.add(message)
        }
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer) {
            incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
        }
    }

    protected class TestRequestInterfaceClientEndpoint : Endpoint(), TestRequestClientEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(TextMessageHandler(incomingMessages))
            session.addMessageHandler(BinaryMessageHandler(incomingMessages))
        }
    }

    protected interface TestRequestClientEndpoint {
        val incomingMessages: MutableList<String>
    }

    private class TextMessageHandler(private val incomingMessages: MutableList<String>) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            incomingMessages.add(message)
        }
    }

    private class BinaryMessageHandler(private val incomingMessages: MutableList<String>) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
        }
    }

}
