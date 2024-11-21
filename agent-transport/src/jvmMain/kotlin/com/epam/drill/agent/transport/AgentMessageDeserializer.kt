package com.epam.drill.agent.transport

interface AgentMessageDeserializer<I, O> {
    fun contentType(): String
    fun deserialize(message: I): O
}