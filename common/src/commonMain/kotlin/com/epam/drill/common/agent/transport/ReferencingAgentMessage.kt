package com.epam.drill.common.agent.transport

import kotlinx.serialization.Serializable

@Serializable
open class ReferencingAgentMessage<T: AgentMessage>(
    open var agentReference: AgentReference?,
    open val agentMessage: T
): AgentMessage()
