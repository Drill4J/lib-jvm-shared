package com.epam.drill.agent.transport

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream

class JsonAgentMessageDeserializer<T>(
    private val strategy: DeserializationStrategy<T>,
): AgentMessageDeserializer<ByteArray, T> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun contentType(): String = "application/json"

    override fun deserialize(message: ByteArray): T = ByteArrayInputStream(message).use {
        json.decodeFromStream(strategy, it)
    }
}