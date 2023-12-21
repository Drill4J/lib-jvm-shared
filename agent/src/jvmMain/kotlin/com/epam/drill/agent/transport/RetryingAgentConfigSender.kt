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
import com.epam.drill.common.agent.configuration.AgentConfig
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import mu.KotlinLogging

class RetryingAgentConfigSender<T>(
    private val transport: AgentMessageTransport<T>,
    private val messageSerializer: AgentMessageSerializer<T>,
    private val destinationMapper: AgentMessageDestinationMapper
) : AgentConfigSender<T> {

    private val logger = KotlinLogging.logger {}
    private val stateListeners = mutableSetOf<TransportStateListener>()
    private var sent = false

    override val configSent: Boolean
        get() = sent

    override fun addStateListener(listener: TransportStateListener) {
        stateListeners.add(listener)
    }

    override fun send(config: AgentConfig) =
        send(messageSerializer.serialize(config), messageSerializer.contentType())

    override fun send(config: T, contentType: String) = thread {
        val destination = destinationMapper.map(AgentMessageDestination("PUT", "agent-config"))
        val cType = contentType.takeIf(String::isNotEmpty) ?: messageSerializer.contentType()
        val send: () -> ResponseStatus = { transport.send(destination, config, cType) }
        val logError: (Throwable) -> Unit = { logger.error(it) { "sendAgentConfig: Attempt is failed: $it" } }
        logger.debug { "sendAgentConfig: Sending to admin server" }
        var success = false
        while(!success) {
            success = runCatching(send).onFailure(logError).getOrElse(::ErrorResponseStatus).success
            if(!success) Thread.sleep(500)
        }
        logger.debug { "sendAgentConfig: Sending to admin server: successful" }
        sent = true
        stateListeners.forEach(TransportStateListener::onStateAlive)
    }

}
