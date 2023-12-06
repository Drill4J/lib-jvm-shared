package com.epam.drill.plugins.test2code.common.transport

import kotlinx.serialization.Serializable
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.plugins.test2code.common.api.AstEntity

@Serializable
data class ClassMetadata(
    val astEntities: List<AstEntity>
): AgentMessage()
