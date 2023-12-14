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
package com.epam.drill.agent.transport

import kotlin.concurrent.thread

class RetryingTransportStateNotifier<T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<T>,
    private val messageQueue: AgentMessageQueue<T>
) : TransportStateNotifier, TransportStateListener {

    private val stateListeners = mutableSetOf<TransportStateListener>()
    private var retrying = false

    override fun addStateListener(listener: TransportStateListener) {
        stateListeners.add(listener)
    }

    override fun onStateAlive() {
    }

    override fun onStateFailed() = synchronized(this) {
        if (!retrying) {
            retrying = true
            retryingThread()
        }
    }

    private fun retryingThread() = thread {
        val pair = messageQueue.remove()
        val send: () -> Unit = { transport.send(pair.first, pair.second, messageSerializer.contentType()) }
        while (runCatching(send).isFailure) {
            Thread.sleep(500)
        }
        retrying = false
        stateListeners.forEach(TransportStateListener::onStateAlive)
    }

}
