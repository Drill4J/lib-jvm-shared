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

import com.epam.drill.common.agent.configuration.AgentConfig

/**
 * An interface to send [AgentConfig] object.
 * Moved to the separate interface as no messages should be sent before [AgentConfig] message.
 *
 * It's used for initial send of [AgentConfig] and has [configSent] property 'false' before that
 * to indicate that transport is in unavailable state.
 *
 * @see [AgentConfig]
 * @see [com.epam.drill.common.agent.transport.AgentMessageSender]
 */
interface AgentConfigSender<T> : TransportStateNotifier {
    val configSent: Boolean
    fun send(config: T, contentType: String = ""): Thread
    fun send(config: AgentConfig): Thread
}
