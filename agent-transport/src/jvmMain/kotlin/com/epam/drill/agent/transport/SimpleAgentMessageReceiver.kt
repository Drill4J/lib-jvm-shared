package com.epam.drill.agent.transport

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageReceiver

class SimpleAgentMessageReceiver<T>(
    private val transport: AgentMessageTransport<*, T>
) : AgentMessageReceiver<T> {

    override fun receive(destination: AgentMessageDestination): T = transport.send(
        destination,
        null
    )
        .takeIf { it.success }?.content
        ?: throw IllegalStateException("Failed to receive message from $destination.")
}