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
package com.epam.drill.agent.websocket

import com.epam.drill.agent.*
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.CountDownLatch
import java.nio.ByteBuffer
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.Session
import mu.KLogger
import mu.KotlinLogging
import com.epam.drill.common.message.Message

class WsClientEndpoint(
    private val messageHandler: MessageHandler.Whole<Message>,
    private val reconnectHandler: WsClientReconnectHandler?
) : Endpoint(), MessageHandler.Whole<ByteArray>, RetentionCallbacks {

    private val latch: CountDownLatch = CountDownLatch(1)
    private val logger: KLogger = KotlinLogging.logger {}
    private var session: Session? = null
    private var onAvailableCallback: () -> Unit = {}
    private var onUnavailableCallback: () -> Unit = {}

    override fun setOnAvailable(callback: () -> Unit) {
        onAvailableCallback = callback
    }

    override fun setOnUnavailable(callback: () -> Unit) {
        onUnavailableCallback = callback
    }

    override fun onOpen(session: Session, config: EndpointConfig) {
        onAvailableCallback.invoke()
        this.session = session
        this.session!!.addMessageHandler(this)
        this.session!!.maxBinaryMessageBufferSize = 33554432
        latch.countDown()
        logger.debug { "onOpen: Session opened, requestURI=${session.requestURI}" }
    }

    override fun onClose(session: Session, reason: CloseReason) {
        onUnavailableCallback.invoke()
        logger.debug {
            "onClose: Session closed, requestURI=${session.requestURI}, " +
                    "closeCode=${reason.closeCode}, reasonPhrase=${reason.reasonPhrase}"
        }
        reconnectHandler?.reconnect()
    }

    override fun onError(session: Session?, e: Throwable) {
        onUnavailableCallback.invoke()
        logger.error(e) { "onError: Error occurred, requestURI=${session?.requestURI}, e=$e" }
    }

    override fun onMessage(bytes: ByteArray) {
        logger.trace { "onMessage: Message received, size=${bytes.size}" }
        val message = ProtoBuf.decodeFromByteArray(Message.serializer(), bytes)
        logger.trace { "onMessage: Message decoded, message=$message" }
        messageHandler.onMessage(message)
    }

    fun sendMessage(bytes: ByteArray) = bytes.runCatching {
        logger.trace { "sendMessage: Sending message, size=${this.size}" }
        session!!.basicRemote.sendBinary(ByteBuffer.wrap(this))
    }

    fun getLatch() = latch

}
