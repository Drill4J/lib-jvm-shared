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

import mu.KotlinLogging
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [AgentMessageSender] implementation with [AgentMessageQueue] for storing
 * serialized messages when transport in unavailable state.
 * @see AgentMessageSender
 * @see AgentMessageQueue
 * @see AgentMessageTransport
 */
open class QueuedAgentMessageSender<T : AgentMessage>(
    private val transport: AgentMessageTransport,
    private val messageSerializer: AgentMessageSerializer<T>,
    private val destinationMapper: AgentMessageDestinationMapper,
    private val messageQueue: AgentMessageQueue<ByteArray>,
    private val messageSendingListener: MessageSendingListener? = null,
    private val exponentialBackoff: ExponentialBackoff = SimpleExponentialBackoff(),
    maxThreads: Int = 1
) : AgentMessageSender<T> {
    private val logger = KotlinLogging.logger {}
    private val executor: ExecutorService = Executors.newFixedThreadPool(maxThreads)
    private val isRunning = AtomicBoolean(true)

    init {
        repeat(maxThreads) {
            executor.submit { processQueue() }
        }
    }

    override fun send(destination: AgentMessageDestination, message: T) {
        val mappedDestination = destinationMapper.map(destination)
        val serializedMessage = messageSerializer.serialize(message)
        val added = isRunning.get() && messageQueue.offer(Pair(mappedDestination, serializedMessage))
        if (!added) {
            handleUnsent(mappedDestination, serializedMessage)
        }
    }

    override fun shutdown() {
        isRunning.set(false)
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }

    /**
     * Processes the message queue.
     * It will try to send the message from a queue to the destination with exponential backoff.
     */
    private fun processQueue() {
        while (isRunning.get() || messageQueue.size() > 0) {
            try {
                val (destination, message) = messageQueue.poll(1, TimeUnit.SECONDS) ?: continue
                exponentialBackoff.tryWithExponentialBackoff { attempt, delay ->
                    tryToSend(destination, message, attempt, delay)
                }.takeIf { !it }?.let {
                    handleUnsent(destination, message)
                }
            } catch (e: Exception) {
                logger.error { "processQueue: Error during queue processing: ${e.message}" }
            }
        }
    }

    /**
     * Tries to send the message to the destination.
     * @param message The serialized message to send.
     * @param destination The destination to which the message should be sent.
     * @param attempt The current attempt number.
     * @param delay The delay in milliseconds before the next attempt.
     */
    private fun tryToSend(
        destination: AgentMessageDestination,
        message: ByteArray,
        attempt: Int,
        delay: Long
    ): Boolean {
        logger.trace {
            val serializedAsString = message.decodeToString()
            "processQueue: Sending to $destination on attempt: $attempt, message: $serializedAsString"
        }
        return transport.send(destination, message, messageSerializer.contentType()).onError { error ->
            logger.debug { "processQueue: Attempt $attempt send to $destination failed. Retrying in ${delay}ms. Error message: $error" }
        }.onSuccess {
            logger.trace {
                val serializedAsString = message.decodeToString()
                "processQueue: Sent to $destination on attempt: $attempt, message: $serializedAsString"
            }
            messageSendingListener?.onSent(destination, message)
        }.success
    }

    /**
     * Handles the case when a message cannot be sent because the queue is full, shutdown, or attempts have been exhausted.
     * @param destination The destination to which the message was intended to be sent.
     * @param message The serialized message that could not be sent.
     */
    private fun handleUnsent(
        destination: AgentMessageDestination,
        message: ByteArray
    ) {
        logger.error {
            val serializedAsString = message.decodeToString()
            "send: Failed to send message, message will be dropped, destination: $destination, message: $serializedAsString"
        }
        messageSendingListener?.onUnsent(destination, message)
    }

}
