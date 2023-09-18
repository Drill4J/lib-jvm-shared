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

import com.epam.drill.agent.ConnectionStatusCallbacks
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.Deflater
import mu.KotlinLogging
import com.epam.drill.common.message.DrillMessage
import com.epam.drill.common.message.DrillMessageWrapper
import com.epam.drill.common.message.Message
import com.epam.drill.common.message.MessageType

object WsClient : WsClientReconnectHandler, ConnectionStatusCallbacks {

    private val endpoint = WsClientEndpoint(WsMessageHandler, this)
    private val logger = KotlinLogging.logger {}
    private var connector: WsClientConnector? = null

    override fun setOnAvailable(callback: () -> Unit) {
        endpoint.setOnAvailable(callback)
    }

    override fun setOnUnavailable(callback: () -> Unit) {
        endpoint.setOnUnavailable(callback)
    }

    init {
        WsMessageHandler.registerTopics()
    }

    override fun reconnect() {
        logger.debug { "reconnect: Starting reconnect attempts" }
        val logError: (Throwable) -> Unit = { logger.error(it) { "reconnect: Reconnect attempt is failed: $it" } }
        val timeout: (Throwable) -> Unit = { Thread.sleep(5000) }
        var connected = false
        while (!connected) {
            connected = runCatching(connector!!::connect).onFailure(logError).onFailure(timeout).isSuccess

        }
        logger.debug { "reconnect: Reconnect is done" }
    }

    @Suppress("UNUSED")
    fun connect(adminUrl: String) {
        connector = WsClientConnector(URI("$adminUrl/agent/attach"), endpoint)
        connector!!.connect()
    }

    fun sendMessage(bytes: ByteArray) {
        logger.trace { "sendMessage: Sending message, size=${bytes.size}" }
        val compressed = compress(bytes)
        logger.trace { "sendMessage: Compressed message size=${compressed.size}" }
        endpoint.sendMessage(compressed)
    }

    fun sendMessage(pluginId: String, content: String) {
        logger.trace { "sendMessage: Sending message, pluginId=$pluginId, content=$content" }
        val data = Json.encodeToString(
            DrillMessageWrapper.serializer(),
            DrillMessageWrapper(pluginId, DrillMessage(content = content))
        )
        sendMessage(Message(MessageType.PLUGIN_DATA, "", data.encodeToByteArray()))
    }

    inline fun <reified T : Any> sendMessage(message: T) {
        sendMessage(ProtoBuf.encodeToByteArray(T::class.serializer(), message))
    }

    private fun compress(input: ByteArray): ByteArray = Deflater(1, true).run {
        ByteArrayOutputStream().use { stream ->
            this.setInput(input)
            this.finish()
            val readBuffer = ByteArray(1024)
            val readed: (Int) -> Boolean = { it > 0 }
            while (!this.finished()) {
                this.deflate(readBuffer).takeIf(readed)?.also { stream.write(readBuffer, 0, it) }
            }
            this.end()
            stream.toByteArray()
        }
    }

}
