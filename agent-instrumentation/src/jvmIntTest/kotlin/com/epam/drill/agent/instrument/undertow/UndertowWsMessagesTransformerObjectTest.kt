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
package com.epam.drill.agent.instrument.undertow

import kotlin.test.Test
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import javax.websocket.ClientEndpoint
import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.websocket.server.ServerEndpoint
import javax.websocket.server.ServerEndpointConfig
import org.xnio.OptionMap
import org.xnio.Xnio
import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentManager
import io.undertow.websockets.jsr.UndertowContainerProvider
import io.undertow.websockets.jsr.WebSocketDeploymentInfo

@Suppress("FunctionName")
class UndertowWsMessagesTransformerObjectTest {

    @Test
    fun `test annotated endpoint sync-remote string with disabled per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote binary with disabled per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote string with disabled per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote binary with disabled per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote string with disabled per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote binary with disabled per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote string with disabled per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote binary with disabled per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote string with client-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote binary with client-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote string with client-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote binary with client-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote string with client-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote binary with client-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote string with client-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote binary with client-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote string with server-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote binary with server-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote string with server-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote binary with server-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote string with server-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote binary with server-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote string with server-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote binary with server-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote string with both-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint sync-remote binary with both-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote string with both-side per-message flag`() = Unit

    @Test
    fun `test annotated endpoint async-remote binary with both-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote string with both-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint sync-remote binary with both-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote string with both-side per-message flag`() = Unit

    @Test
    fun `test interface endpoint async-remote binary with both-side per-message flag`() = Unit

    private fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(TestRequestServerAnnotatedEndpoint::class.java)
        .let { withWebSocketServer(it, block) }

    private fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(ServerEndpointConfig.Builder.create(TestRequestServerInterfaceEndpoint::class.java, "/").build())
        .let { withWebSocketServer(it, block) }

    private fun withWebSocketServer(info: WebSocketDeploymentInfo, block: (String) -> Unit) = Undertow.builder().run {
        val wsDeploymentManager = webSocketDeploymentManager(info)
        this.addHttpListener(0, "localhost")
        this.setHandler(wsDeploymentManager.also(DeploymentManager::deploy).let(DeploymentManager::start))
        val server = this.build()
        try {
            server.start()
            block("ws://localhost:${(server.listenerInfo[0].address as InetSocketAddress).port}")
        } finally {
            wsDeploymentManager.stop()
            wsDeploymentManager.undeploy()
            server.stop()
        }
    }

    private fun webSocketDeploymentManager(info: WebSocketDeploymentInfo) = Servlets.defaultContainer().run {
        val worker = Xnio.getInstance().createWorker(OptionMap.EMPTY)
        val deployment = Servlets.deployment()
            .setClassLoader(this::class.java.classLoader)
            .setContextPath("/")
            .setDeploymentName("test-websockets")
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info.setWorker(worker))
        this.addDeployment(deployment)
    }

    private fun connectToWebsocketAnnotatedEndpoint(endpoint: String) = TestRequestClientAnnotatedEndpoint().run {
        val session = TestUndertowContainerProvider().container.connectToServer(this, URI(endpoint))
        this to session
    }

    private fun connectToWebsocketInterfaceEndpoint(endpoint: String) = TestRequestClientInterfaceEndpoint().run {
        val session = TestUndertowContainerProvider().container
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(endpoint))
        this to session
    }

    @Suppress("unused")
    @ServerEndpoint(value = "/")
    class TestRequestServerAnnotatedEndpoint {
        @OnMessage
        fun onTextMessage(message: String, session: Session) {
            session.basicRemote.sendText(message)
        }
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer, session: Session) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
        }
    }

    class TestRequestServerInterfaceEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message ->
                session.basicRemote.sendText(message)
            }
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                val text = ByteArray(message.limit()).also(message::get).decodeToString()
                session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
            }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ServerTextMessageHandler(session))
            session.addMessageHandler(ServerBinaryMessageHandler(session))
        }
    }

    private class ServerTextMessageHandler(private val session: Session) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            session.basicRemote.sendText(message)
        }
    }

    private class ServerBinaryMessageHandler(private val session: Session) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
        }
    }

    private class TestUndertowContainerProvider : UndertowContainerProvider() {
        public override fun getContainer(): WebSocketContainer = super.getContainer()
    }

    @Suppress("unused")
    @ClientEndpoint
    private class TestRequestClientAnnotatedEndpoint : TestRequestClientEndpoint {
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

    private class TestRequestClientInterfaceEndpoint : Endpoint(), TestRequestClientEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message ->
                incomingMessages.add(message)
            }
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
            }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ClientTextMessageHandler(incomingMessages))
            session.addMessageHandler(ClientBinaryMessageHandler(incomingMessages))
        }
    }

    private interface TestRequestClientEndpoint {
        val incomingMessages: MutableList<String>
    }

    private class ClientTextMessageHandler(private val incomingMessages: MutableList<String>) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            incomingMessages.add(message)
        }
    }

    private class ClientBinaryMessageHandler(private val incomingMessages: MutableList<String>) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
        }
    }

}
