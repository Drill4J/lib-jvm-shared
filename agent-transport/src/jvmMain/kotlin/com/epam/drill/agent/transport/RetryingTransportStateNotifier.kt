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
import mu.KotlinLogging

class RetryingTransportStateNotifier<T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<*, *>,
    private val messageQueue: AgentMessageQueue<T>
) : TransportStateNotifier, TransportStateListener {

    private val logger = KotlinLogging.logger {}
    private val stateListeners = mutableSetOf<TransportStateListener>()
    internal var retrying = false
    internal var retryingThread: Thread? = null

    override fun addStateListener(listener: TransportStateListener) {
        stateListeners.add(listener)
    }

    override fun onStateAlive() {
        logger.debug { "onStateAlive: Alive event received" }
    }

    override fun onStateFailed() = synchronized(this) {
        logger.debug { "onStateFailed: Failed event received, current retrying state: $retrying" }
        if (!retrying) {
            messageQueue.element()
            retrying = true
            retryingThread = retryingThread()
        }
    }

    private fun retryingThread() = thread {
        val pair = messageQueue.remove()
        val send: () -> Unit = { transport.send(pair.first, pair.second, messageSerializer.contentType()) }
        val logError: (Throwable) -> Unit = { logger.error(it) { "retryingThread: Attempt is failed: $it" } }
        logger.debug { "retryingThread: Check state using first queued message" }
        while (runCatching(send).onFailure(logError).isFailure) {
            Thread.sleep(500)
        }
        retrying = false
        stateListeners.forEach(TransportStateListener::onStateAlive)
        logger.debug { "retryingThread: Check state using first queued message: success" }
    }

}
