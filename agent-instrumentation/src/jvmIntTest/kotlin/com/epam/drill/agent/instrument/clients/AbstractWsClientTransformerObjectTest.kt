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
import javax.websocket.HandshakeResponse
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpoint
import javax.websocket.server.ServerEndpointConfig
import org.glassfish.tyrus.core.TyrusServerEndpointConfigurator
import org.glassfish.tyrus.server.Server
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.common.agent.request.DrillRequest

@Suppress("FunctionName")
abstract class AbstractWsClientTransformerObjectTest {

    @Test
    fun `test request-response with empty thread session and headers data`() = withWebSocketServer {
        TestRequestHolder.remove()
        val handshake = connectToWebsocketEndpoint(it)
        val handshakeHeaders = handshake.first
        val session = handshake.second
        val responseBody = sendToWebSocketEndpoint(session)
        session.close()
        assertNull(TestRequestHolder.retrieve())
        assertNull(handshakeHeaders["drill-session-id"])
        assertNull(handshakeHeaders["drill-header-data"])
        assertEquals("test-request", responseBody)
    }

    @Test
    fun `test request with existing thread session data`() = withWebSocketServer {
        TestRequestHolder.store(DrillRequest("session-123", mapOf("drill-header-data" to "test-data")))
        val handshake = connectToWebsocketEndpoint(it)
        val handshakeHeaders = handshake.first
        val session = handshake.second
        val responseBody = sendToWebSocketEndpoint(session)
        session.close()
        assertEquals("session-123", handshakeHeaders["drill-session-id"])
        assertEquals("test-data", handshakeHeaders["drill-header-data"])
        assertEquals("test-request", responseBody)
    }

    protected abstract fun connectToWebsocketEndpoint(endpoint: String): Pair<Map<String, String>, Session>

    protected open fun sendToWebSocketEndpoint(session: Session, body: String = "test-request"): String? {
        var response: String? = null
        session.addMessageHandler(String::class.java) { msg -> response = msg }
        session.basicRemote.sendText(body)
        Thread.sleep(500)
        return response
    }

    private fun withWebSocketServer(block: (String) -> Unit) = Server(TestRequestServerEndpoint::class.java).run {
        try {
            this.start()
            block("ws://localhost:${this.port}")
        } finally {
            this.stop()
        }
    }

    @Suppress("unused")
    @ServerEndpoint(value = "/", configurator = TestRequestConfigurator::class)
    private class TestRequestServerEndpoint {
        @OnOpen
        fun onOpen(session: Session, config: ServerEndpointConfig) = (config.configurator as TestRequestConfigurator)
            .headers
            .map { (key, value) -> "${key}=${value}" }
            .joinToString("\n", "session-headers:\n")
            .let(session.basicRemote::sendText)
        @OnMessage
        fun onTextMessage(message: String, session: Session) = session.basicRemote.sendText(message)
    }

    private class TestRequestConfigurator : TyrusServerEndpointConfigurator() {
        val headers = mutableMapOf<String, String>()
        override fun modifyHandshake(sec: ServerEndpointConfig, req: HandshakeRequest, resp: HandshakeResponse) =
            req.headers.forEach { headers[it.key] = it.value.joinToString(",") }
    }

}
