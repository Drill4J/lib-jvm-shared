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

/**
 * A listener interface to receive notification about transport state changes.
 * Supports both stateful (like websocket sessions) and stateless transports (like HTTP calls).
 *
 * It's used to notify [com.epam.drill.agent.common.transport.AgentMessageSender]
 * about [AgentMessageTransport] state changes using [TransportStateNotifier].
 *
 * @see TransportStateNotifier
 * @see AgentMessageTransport
 * @see com.epam.drill.agent.common.transport.AgentMessageSender
 */
interface TransportStateListener {
    fun onStateAlive()
    fun onStateFailed()
}
