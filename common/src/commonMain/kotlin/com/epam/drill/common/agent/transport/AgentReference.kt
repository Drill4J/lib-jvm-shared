package com.epam.drill.common.agent.transport

import kotlinx.serialization.Serializable

@Serializable
data class AgentReference(
    val instanceId: String,
    val agentId: String,
    val groupId: String,
    val buildVersion: String
)
