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
import mu.KotlinLogging
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.agent.transport.ResponseStatus

private const val TRANSPORT_ERR = "Transport is in unavailable state"

/**
 * A [AgentMessageSender] implementation with [AgentMessageQueue] for storing
 * serialized messages when transport in unavailable state.
 *
 * It also implements [AgentMetadataSender] for initial availability status.
 *
 * The [AgentMessageSender]'s [available] state may be changed to 'false' both by
 * external [TransportStateNotifier] and exception during message send by [AgentMessageTransport].
 *
 * The [AgentMessageSender]'s [available] state may be changed to 'true' only by
 * external [TransportStateNotifier].
 *
 * In case of exception during message send by [AgentMessageTransport]
 * it also notifies [TransportStateListener] if it's defined.
 *
 * @see AgentMessageSender
 * @see AgentMessageQueue
 * @see AgentMessageTransport
 * @see AgentMetadataSender
 * @see TransportStateNotifier
 * @see TransportStateListener
 */
open class QueuedAgentMessageSender<M : AgentMessage, T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<M, T>,
    private val destinationMapper: AgentMessageDestinationMapper,
    transportStateNotifier: TransportStateNotifier,
    private val transportStateListener: TransportStateListener?,
    private val messageQueue: AgentMessageQueue<T>
) : AgentMessageSender<M>, TransportStateListener {

    private val logger = KotlinLogging.logger {}

    private var alive = true
    internal var sendingThread: Thread? = null

    init {
        transportStateNotifier.addStateListener(this)
    }

    override val available
        get() = alive

    override fun send(destination: AgentMessageDestination, message: M): ResponseStatus {
        val mappedDestination = destinationMapper.map(destination)
        val serializedMessage = messageSerializer.serialize(message)
        val store: (Throwable) -> Unit = {
            logger.trace {
                val serializedAsString = messageSerializer.stringValue(serializedMessage)
                "send: Storing for ${mappedDestination}:\n$serializedAsString"
            }
            messageQueue.offer(Pair(mappedDestination, serializedMessage))
        }
        synchronized(messageQueue) {
            if (!available) return IOException(TRANSPORT_ERR).also(store).let(::ErrorResponseStatus)
        }
        return Pair(mappedDestination, serializedMessage)
            .runCatching(::send).onFailure(store).onFailure(::failure).getOrElse(::ErrorResponseStatus)
    }

    override fun onStateAlive() {
        logger.debug { "onStateAlive: Alive event received" }
        sendingThread = sendQueue()
    }

    override fun onStateFailed() = synchronized(messageQueue) {
        logger.debug { "onStateFailed: Failed event received" }
        alive = false
    }

    private fun send(message: Pair<AgentMessageDestination, T>): ResponseStatus {
        val contentType = messageSerializer.contentType()
        logger.trace {
            val serializedAsString = messageSerializer.stringValue(message.second)
            "send: Sending to ${message.first}, contentType=$contentType:\n$serializedAsString"
        }
        return transport.send(message.first, message.second, contentType)
    }

    private fun failure(t: Throwable) = synchronized(messageQueue) {
        logger.error(t) { "failure: Error during message sending, current availability state: $available" }
        if (available) {
            alive = false
            transportStateListener?.onStateFailed()
        }
    }

    private fun sendQueue() = thread {
        var success = true
        var message = peekMessage()
        logger.debug { "sendQueue: Starting queue processing, queue size: ${messageQueue.size()}" }
        while (success && message != null) {
            success = message.runCatching(::send).onFailure(::failure).isSuccess
            if (success) {
                messageQueue.remove()
                message = peekMessage()
            }
        }
        logger.debug { "sendQueue: Done queue processing, queue size: ${messageQueue.size()}" }
    }

    private fun peekMessage() = synchronized(messageQueue) {
        val message = messageQueue.peek()
        if(message == null) alive = true
        message
    }

}
