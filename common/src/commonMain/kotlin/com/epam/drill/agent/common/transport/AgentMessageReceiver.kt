package com.epam.drill.agent.common.transport

interface AgentMessageReceiver<T> {
    fun receive(destination: AgentMessageDestination): T
}