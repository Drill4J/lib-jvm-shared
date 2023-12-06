package com.epam.drill.common.agent.transport

interface AgentMessageSender {
    fun <T: AgentMessage> send(destination: AgentMessageDestination, message: T): ResponseStatus
    fun isTransportAvailable(): Boolean
}
