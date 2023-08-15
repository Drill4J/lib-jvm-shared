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
package com.epam.drill.transport

import kotlin.collections.plus
import kotlin.native.SharedImmutable
import kotlin.native.ThreadLocal
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlinx.atomicfu.update
import kotlinx.cinterop.*
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.posix.memcpy
import platform.posix.size_t
import mu.KotlinLogging
import com.epam.drill.transport.internal.FrameOutputStream
import com.epam.drill.transport.internal.client
import com.epam.drill.transport.internal.lwsEventsDescription
import com.epam.drill.transport.internal.messages
import com.epam.drill.transport.internal.onBinariesCallbacks
import com.epam.drill.transport.internal.onCloseCallbacks
import com.epam.drill.transport.internal.onErrorCallbacks
import com.epam.drill.transport.internal.onOpenCallbacks
import com.epam.drill.websocket.gen.*

private const val BUFF_SIZE_MAX_VALUE = 1024 * 100000

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.transport.WSClientFactory")

@ThreadLocal
private var readFrame = byteArrayOf()

@ThreadLocal
private lateinit var connectionSettings: ConnectionSettings

@ThreadLocal
private lateinit var context: CPointer<lws_context>

@ThreadLocal
private lateinit var sul: lws_sorted_usec_list_t

@ThreadLocal
private val clientConnect = staticCFunction { _: CPointer<lws_sorted_usec_list_t>? ->
    initRuntimeIfNeeded()
    memScoped {
        connectionSettings.let {
            val i = alloc<lws_client_connect_info>()
            i.context = context
            i.port = it.url.port
            i.address = it.url.host?.cstr?.getPointer(this)
            i.path = it.url.path.cstr.getPointer(this)
            i.host = i.address
            i.origin = i.address
            i.protocol = "custom".cstr.getPointer(this)
            i.local_protocol_name = "lws-minimal-client".cstr.getPointer(this)
            if (lws_client_connect_via_info(i.ptr) != null) {
                logger.debug { "successfully connected" }
            }
        }
    }
}.freeze()

@ThreadLocal
private val connectionWatcher =
    staticCFunction { wsi: CPointer<lws>?, reason: lws_callback_reasons, _: COpaquePointer?, input: COpaquePointer?, len: size_t ->
        initRuntimeIfNeeded()
        val wrkId = Worker.current.id
        when (reason) {
            LWS_CALLBACK_CLIENT_ESTABLISHED -> {
                client.update {
                    it + (wrkId to wsi!!)
                }
                onOpenCallbacks.forEach { it() }
            }
            LWS_CALLBACK_CLIENT_APPEND_HANDSHAKE_HEADER -> {
                memScoped {
                    headers().forEach { (key, value) ->
                        addHeader(
                            wsi, input, len, "$key:".cstr.getPointer(this).reinterpret(),
                            value.cstr.getPointer(this).reinterpret(), value.length
                        )
                    }

                }
            }
            LWS_CALLBACK_CLIENT_CONNECTION_ERROR, LWS_CALLBACK_CLOSED, LWS_CALLBACK_CLIENT_CLOSED, LWS_CALLBACK_WSI_DESTROY -> {
                when (reason) {
                    LWS_CALLBACK_CLIENT_CONNECTION_ERROR -> {
                        onErrorCallbacks.forEach {
                            val errorString = ByteArray(len.convert()).apply {
                                usePinned {
                                    memcpy(it.addressOf(0), input, len.convert())
                                }
                            }.decodeToString()
                            it(errorString)
                        }
                    }
                    LWS_CALLBACK_CLIENT_CLOSED -> {
                        onCloseCallbacks.forEach {
                            it()
                        }
                    }
                }
                client.update { it.remove(wrkId) }
                //reconnect ws
                lws_sul_schedule(
                    context,
                    0,
                    sul.ptr,
                    clientConnect,
                    3 * LWS_USEC_PER_SEC
                )
            }
            LWS_CALLBACK_CLIENT_RECEIVE -> {
                wsi?.let { read(it, len, input) }
            }
            LWS_CALLBACK_CLIENT_WRITEABLE -> {
                wsi?.let { writeLocal(wsi) }
            }
            else -> {
                lwsEventsDescription[reason]?.let { logger.trace { "Event description: $it" } }
            }
        }
        Worker.current.processQueue()
        0
    }.freeze()

private fun read(
    wsi: CPointer<lws>?,
    len: size_t,
    input: COpaquePointer?
) {
    val readBytes = ByteArray(len.convert()).apply {
        usePinned {
            memcpy(it.addressOf(0), input, len.convert())
        }
    }
    if (lws_is_final_fragment(wsi) == 1) {
        readFrame += readBytes
        onBinariesCallbacks.forEach { it(readFrame) }
        readFrame = byteArrayOf()
    } else {
        readFrame += readBytes
    }
}

private fun writeLocal(wsi: CPointer<lws>?) {
    val wrkId = Worker.current.id
    val channel = messages.value[wrkId]
    channel?.firstOrNull()?.let { ptr ->
        val readNext = ptr.readNext()
        val bytes = readNext.bytes
        bytes.usePinned {
            val addressOf = it.addressOf(0) + LWS_PRE
            val lwsWrite = lws_write(
                wsi,
                addressOf?.reinterpret(),
                (bytes.size - LWS_PRE).convert(),
                lws_write_ws_flags(
                    LWS_WRITE_BINARY.toInt(),
                    if (readNext.isFirstFrame) 1 else 0,
                    if (readNext.isLastFrame) 1 else 0
                ).convert()
            )
            logger.trace { "sent $lwsWrite" }
        }
        if (ptr.isFinish()) {
            removeFirst(wrkId, ptr)
        }
    }
    if (messages.value[wrkId]?.isNotEmpty() == true)
        lws_callback_on_writable(wsi)
}

private fun removeFirst(wrkId: Int, ptr: FrameOutputStream) {
    messages.update { prev ->
        prev[wrkId]?.let {
            messages.value.put(wrkId, it - ptr)
        } ?: persistentHashMapOf()
    }
}

object WSClientFactory {

    fun createClient(
        url: URL,
        rxBufferSize: Int = BUFF_SIZE_MAX_VALUE,
        txBufferSize: Int = BUFF_SIZE_MAX_VALUE
    ): WSClient {
        val lwsClient = WSClient()
        lwsClient.launch {
            messages.update { it.put(Worker.current.id, persistentListOf()) }
            memScoped {
                sul = alloc()
                val set = ConnectionSettings(
                    url,
                    rxBufferSize = rxBufferSize,
                    txBufferSize = txBufferSize
                )
                connectionSettings = set.freeze()
                val alloc = cValue<lws_protocols> {
                    this.name = "lws-minimal-client".cstr.getPointer(this@memScoped)
                    callback = connectionWatcher
                    rx_buffer_size = set.rxBufferSize.toULong()
                    tx_packet_size = set.txBufferSize.toULong()

                }
                val createContext = createContext(BUFF_SIZE_MAX_VALUE, alloc) ?: throw RuntimeException()
                context = createContext
                lws_sul_schedule(context, 0, sul.ptr, clientConnect, 1)
                var n = 0
                while (n >= 0 && interrupted != 1) {
                    n = lws_service(context, 0)
                    delay(1)
                }
                lws_context_destroy(context)
            }
        }
        return lwsClient.freeze()
    }

}

private data class ConnectionSettings(
    val url: URL,
    val rxBufferSize: Int = 0,
    val txBufferSize: Int = 0
)
