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
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus

class RetryingAgentMetadataSender<T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<in AgentMetadata, T>,
    private val destinationMapper: AgentMessageDestinationMapper
) : AgentMetadataSender<T> {

    private val logger = KotlinLogging.logger {}
    private val stateListeners = mutableSetOf<TransportStateListener>()

    override fun addStateListener(listener: TransportStateListener) {
        stateListeners.add(listener)
    }

    override fun send(metadata: AgentMetadata) =
        send(messageSerializer.serialize(metadata), messageSerializer.contentType())

    override fun send(metadata: T, contentType: String) = thread {
        val destination = destinationMapper.map(AgentMessageDestination("PUT", "instances"))
        val cType = contentType.takeIf(String::isNotEmpty) ?: messageSerializer.contentType()
        val send: () -> ResponseStatus = { transport.send(destination, metadata, cType) }
        val logError: (Throwable) -> Unit = { logger.error(it) { "send: Attempt is failed: $it" } }
        val logResp: (ResponseStatus) -> Unit = { logger.debug { "send: HTTP status received: ${it.statusObject}" } }
        logger.debug { "send: Sending to admin server" }
        var success = false
        while(!success) {
            success = runCatching(send).onFailure(logError).onSuccess(logResp).getOrElse(::ErrorResponseStatus).success
            if(!success) Thread.sleep(500)
        }
        logger.debug { "send: Sending to admin server: successful" }
        stateListeners.forEach(TransportStateListener::onStateAlive)
    }

}
