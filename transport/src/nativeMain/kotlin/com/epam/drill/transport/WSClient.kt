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

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SingleThreadDispatcher
import kotlinx.coroutines.newSingleThreadContext
import com.epam.drill.transport.internal.FrameOutputStream
import com.epam.drill.transport.internal.client
import com.epam.drill.transport.internal.messages
import com.epam.drill.transport.internal.onBinariesCallbacks
import com.epam.drill.transport.internal.onCloseCallbacks
import com.epam.drill.transport.internal.onErrorCallbacks
import com.epam.drill.transport.internal.onOpenCallbacks
import com.epam.drill.websocket.gen.lws_callback_on_writable

class WSClient : CoroutineScope {

    fun onBinaryMessage(block: (ByteArray) -> Unit) {
        wrk.execute(TransferMode.UNSAFE, { block.freeze() }) {
            onBinariesCallbacks.plusAssign(it)
        }
    }

    fun onClose(block: () -> Unit) {
        wrk.execute(TransferMode.UNSAFE, { block.freeze() }) {
            onCloseCallbacks += it
        }
    }

    fun onError(block: (String) -> Unit) {
        wrk.execute(TransferMode.UNSAFE, { block.freeze() }) {
            onErrorCallbacks += it
        }
    }

    fun onOpen(block: () -> Unit) {
        wrk.execute(TransferMode.UNSAFE, { block.freeze() }) {
            onOpenCallbacks += it
        }
    }

    fun send(message: ByteArray) {
        val wrkId = wrk.id
        addLast(wrkId, message)
        client.value[wrkId]?.let {
            lws_callback_on_writable(it)
        }

    }

    private val wrk: Worker
        get() = coroutineContext.worker

    override val coroutineContext: SingleThreadDispatcher = newSingleThreadContext("tst")

}

private fun addLast(wrkId: Int, message: ByteArray) {
    messages.update { prev ->
        prev[wrkId]?.let {
            messages.value.put(wrkId, it + FrameOutputStream(message))
        } ?: persistentHashMapOf()
    }
}
