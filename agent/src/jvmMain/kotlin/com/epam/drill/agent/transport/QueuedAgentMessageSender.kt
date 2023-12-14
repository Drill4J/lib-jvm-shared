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
import java.io.IOException
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.agent.transport.ResponseStatus

private const val TRANSPORT_ERR = "Transport is in unavailable state"

//TODO add java-docs
open class QueuedAgentMessageSender<T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<T>,
    private val destinationMapper: AgentMessageDestinationMapper,
    agentConfigSender: AgentConfigSender<T>,
    transportStateNotifier: TransportStateNotifier,
    private val transportStateListener: TransportStateListener?,
    private val messageQueue: AgentMessageQueue<T>
) : AgentMessageSender, AgentConfigSender<T> by agentConfigSender, TransportStateListener {

    private var alive = true

    init {
        agentConfigSender.addStateListener(this)
        transportStateNotifier.addStateListener(this)
    }

    override val available
        get() = configSent && alive

    override fun send(destination: AgentMessageDestination, message: AgentMessage): ResponseStatus {
        val mappedDestination = destinationMapper.map(destination)
        val serializedMessage = messageSerializer.serialize(message)
        val store: (Throwable) -> Unit = { messageQueue.offer(Pair(mappedDestination, serializedMessage)) }
        synchronized(messageQueue) {
            if (!available) return IOException(TRANSPORT_ERR).also(store).let(::ErrorResponseStatus)
        }
        return Pair(mappedDestination, serializedMessage)
            .runCatching(::send).onFailure(store).onFailure(::failure).getOrElse(::ErrorResponseStatus)
    }

    override fun onStateAlive() {
        sendQueue()
    }

    override fun onStateFailed() = synchronized(messageQueue) {
        alive = false
    }

    private fun send(message: Pair<AgentMessageDestination, T>) =
        transport.send(message.first, message.second, messageSerializer.contentType())

    private fun failure(t: Throwable): Unit = synchronized(messageQueue) {
        if (available) {
            alive = false
            transportStateListener?.onStateFailed()
        }
    }

    private fun sendQueue() = thread {
        var success = true
        var message = peekMessage()
        while (success && message != null) {
            success = message.runCatching(::send).onFailure(::failure).isSuccess
            if (success) {
                messageQueue.remove()
                message = peekMessage()
            }
        }
    }

    private fun peekMessage() = synchronized(messageQueue) {
        val message = messageQueue.peek()
        if(message == null) alive = true
        message
    }

}
