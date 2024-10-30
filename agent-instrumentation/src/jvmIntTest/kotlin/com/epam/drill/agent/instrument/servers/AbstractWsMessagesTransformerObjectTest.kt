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
import kotlin.test.assertNull
import java.nio.ByteBuffer
import javax.websocket.ClientEndpoint
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.server.ServerEndpoint
import com.epam.drill.agent.instrument.TestPayloadProcessor
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.common.request.DrillRequest

@Suppress("FunctionName")
abstract class AbstractWsMessagesTransformerObjectTest {

    @Test
    fun `test annotated endpoint sync-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "text", "basic"
    )

    @Test
    fun `test annotated endpoint sync-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "binary", "basic"
    )

    @Test
    fun `test annotated endpoint async-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "text", "async"
    )

    @Test
    fun `test annotated endpoint async-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "binary", "async"
    )

    @Test
    fun `test interface endpoint sync-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "text", "basic"
    )

    @Test
    fun `test interface endpoint sync-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "binary", "basic"
    )

    @Test
    fun `test interface endpoint async-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "text", "async"
    )

    @Test
    fun `test interface endpoint async-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "binary", "async"
    )

    @Test
    fun `test annotated endpoint sync-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "text", "basic"
    )

    @Test
    fun `test annotated endpoint sync-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "binary", "basic"
    )

    @Test
    fun `test annotated endpoint async-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "text", "async"
    )

    @Test
    fun `test annotated endpoint async-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "binary", "async"
    )

    @Test
    fun `test interface endpoint sync-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "text", "basic"
    )

    @Test
    fun `test interface endpoint sync-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "binary", "basic"
    )

    @Test
    fun `test interface endpoint async-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "text", "async"
    )

    @Test
    fun `test interface endpoint async-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "binary", "async"
    )

    protected abstract fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit)

    protected abstract fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit)

    protected abstract fun connectToWebsocketAnnotatedEndpoint(address: String): Pair<TestRequestEndpoint, Session>

    protected abstract  fun connectToWebsocketInterfaceEndpoint(address: String): Pair<TestRequestEndpoint, Session>

    private fun testPerMessageRequests(
        withServer: (block: (String) -> Unit) -> Unit,
        withConnection: (String) -> Pair<TestRequestEndpoint, Session>,
        perMessageEnabled: Boolean,
        payloadType: String,
        sendType: String
    ) = withServer { address ->
        TestRequestEndpoint.incomingMessages.clear()
        TestRequestEndpoint.incomingContexts.clear()
        TestRequestHolder.remove()
        TestPayloadProcessor.enabled = perMessageEnabled
        val responses = callWebSocketEndpoint(withConnection, address, payloadType, sendType)
        assertEquals(10, TestRequestEndpoint.incomingMessages.size)
        assertEquals(10, TestRequestEndpoint.incomingContexts.size)
        assertEquals(10, responses.first.size)
        assertEquals(10, responses.second.size)
        TestRequestEndpoint.incomingMessages.forEachIndexed { i, message ->
            assertEquals("test-request-$i", message)
        }
        TestRequestEndpoint.incomingContexts.forEachIndexed { i, drillRequest ->
            if(perMessageEnabled) {
                assertNotNull(drillRequest)
                assertEquals("test-request-$i-session", drillRequest.drillSessionId)
                assertEquals("test-request-$i-data", drillRequest.headers["drill-data"])
            } else {
                assertNull(drillRequest)
            }
        }
        responses.first.forEachIndexed { i, message ->
            assertEquals("test-request-$i-response", message)
        }
        responses.second.forEachIndexed { i, drillRequest ->
            if(perMessageEnabled) {
                assertNotNull(drillRequest)
                assertEquals("test-request-$i-response-session", drillRequest.drillSessionId)
                assertEquals("test-request-$i-response-data", drillRequest.headers["drill-data"])
            } else {
                assertNull(drillRequest)
            }
        }
    }

    private fun callWebSocketEndpoint(
        withConnection: (String) -> Pair<TestRequestEndpoint, Session>,
        address: String,
        payloadType: String,
        sendType: String,
        body: String = "test-request-",
        count: Int = 10
    ) = withConnection(address).run {
        val session = this.second
        when (payloadType) {
            "text" -> when (sendType) {
                "basic" -> (0 until count).map(body::plus).forEach {
                    TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data")))
                    session.basicRemote.sendText(it)
                    TestRequestHolder.remove()
                }
                "async" -> (0 until count).map(body::plus).forEach {
                    TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data")))
                    session.asyncRemote.sendText(it)
                    TestRequestHolder.remove()
                }
            }
            "binary" -> when (sendType) {
                "basic" -> (0 until count).map(body::plus).forEach {
                    TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data")))
                    session.basicRemote.sendBinary(ByteBuffer.wrap(it.encodeToByteArray()))
                    TestRequestHolder.remove()
                }
                "async" -> (0 until count).map(body::plus).forEach {
                    TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data")))
                    session.asyncRemote.sendBinary(ByteBuffer.wrap(it.encodeToByteArray()))
                    TestRequestHolder.remove()
                }
            }
        }
        Thread.sleep(1000)
        session.close()
        this.first.incomingMessages to this.first.incomingContexts
    }

    interface TestRequestEndpoint {
        companion object {
            val incomingMessages = mutableListOf<String>()
            val incomingContexts = mutableListOf<DrillRequest?>()
        }
        val incomingMessages: MutableList<String>
        val incomingContexts: MutableList<DrillRequest?>
        fun processIncoming(message: String, session: Session?) {
            incomingMessages.add(message)
            incomingContexts.add(TestRequestHolder.retrieve())
            if (session != null) {
                TestRequestHolder.store(DrillRequest("$message-response-session", mapOf("drill-data" to "$message-response-data")))
                session.basicRemote.sendText("$message-response")
                TestRequestHolder.remove()
            }
        }
        fun processIncoming(message: ByteBuffer, session: Session?) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            incomingMessages.add(text)
            incomingContexts.add(TestRequestHolder.retrieve())
            if (session != null) {
                TestRequestHolder.store(DrillRequest("$text-response-session", mapOf("drill-data" to "$text-response-data")))
                session.basicRemote.sendBinary(ByteBuffer.wrap("$text-response".encodeToByteArray()))
                TestRequestHolder.remove()
            }
        }
    }

    @Suppress("unused")
    @ServerEndpoint(value = "/")
    class TestRequestServerAnnotatedEndpoint : TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        @OnMessage
        fun onTextMessage(message: String, session: Session) = processIncoming(message, session)
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer, session: Session) = processIncoming(message, session)
    }

    class TestRequestServerInterfaceEndpoint : Endpoint(), TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message -> processIncoming(message, session) }
            session.addMessageHandler(ByteBuffer::class.java) { message -> processIncoming(message, session) }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ServerTextMessageHandler(session, incomingMessages, incomingContexts))
            session.addMessageHandler(ServerBinaryMessageHandler(session, incomingMessages, incomingContexts))
        }
    }

    @Suppress("unused")
    @ClientEndpoint
    class TestRequestClientAnnotatedEndpoint : TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        @OnMessage
        fun onTextMessage(message: String) = processIncoming(message, null)
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer) = processIncoming(message, null)
    }

    class TestRequestClientInterfaceEndpoint : Endpoint(), TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message -> processIncoming(message, null) }
            session.addMessageHandler(ByteBuffer::class.java) { message -> processIncoming(message, null) }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ClientTextMessageHandler(incomingMessages, incomingContexts))
            session.addMessageHandler(ClientBinaryMessageHandler(incomingMessages, incomingContexts))
        }
    }

    private class ServerTextMessageHandler(
        private val session: Session,
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<String>, TestRequestEndpoint {
        override fun onMessage(message: String) = processIncoming(message, session)
    }

    private class ServerBinaryMessageHandler(
        private val session: Session,
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<ByteBuffer>, TestRequestEndpoint {
        override fun onMessage(message: ByteBuffer) = processIncoming(message, session)
    }

    private class ClientTextMessageHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<String>, TestRequestEndpoint {
        override fun onMessage(message: String) = processIncoming(message, null)
    }

    private class ClientBinaryMessageHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<ByteBuffer>, TestRequestEndpoint {
        override fun onMessage(message: ByteBuffer) = processIncoming(message, null)
    }

}
