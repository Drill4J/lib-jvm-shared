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

import com.epam.drill.common.agent.configuration.AgentMetadata

/**
 * An interface to send [AgentMetadata] object.
 * Moved to the separate interface as no messages should be sent before [AgentMetadata] message.
 *
 * It's used for initial send of [AgentMetadata] and has [metadataSent] property 'false' before that
 * to indicate that transport is in unavailable state.
 *
 * @see [AgentMetadata]
 * @see [com.epam.drill.common.agent.transport.AgentMessageSender]
 */
interface AgentMetadataSender<T> : TransportStateNotifier {
    fun send(metadata: T, contentType: String = ""): Thread
    fun send(metadata: AgentMetadata): Thread
}
