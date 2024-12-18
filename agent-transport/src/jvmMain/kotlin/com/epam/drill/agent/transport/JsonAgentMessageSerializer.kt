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

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination

class JsonAgentMessageSerializer<M : AgentMessage>(
    private val serializer: SerializationStrategy<M>? = null
) : AgentMessageSerializer<M, ByteArray> {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun contentType(): String = "application/json"

    @Suppress("HasPlatformType")
    override fun serialize(message: M) = ByteArrayOutputStream().use {
        json.encodeToStream(serializer ?: serializer(message.javaClass), message, it)
        it.toByteArray()
    }

    override fun sizeOf(destination: AgentMessageDestination) = destination.type.length + destination.target.length

    override fun sizeOf(serialized: ByteArray) = serialized.size

    override fun stringValue(serialized: ByteArray) = "\n${serialized.decodeToString().prependIndent("\t")}"

}
