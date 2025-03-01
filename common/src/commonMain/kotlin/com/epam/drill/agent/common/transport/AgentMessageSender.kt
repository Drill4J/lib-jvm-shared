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
package com.epam.drill.agent.common.transport

/**
 * An interface to send [AgentMessage] objects to [AgentMessageDestination].
 * It has [available] property to indicate transport state.
 *
 * It should be provided to all agent message producers.
 *
 * @see [AgentMessage]
 * @see [AgentMessageDestination]
 */
interface AgentMessageSender<T> {
    fun send(destination: AgentMessageDestination, message: T)
    fun shutdown() {}
}
