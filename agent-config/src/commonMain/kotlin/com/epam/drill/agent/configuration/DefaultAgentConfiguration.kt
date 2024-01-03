package com.epam.drill.agent.configuration

import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.configuration.AgentParameters

expect class DefaultAgentConfiguration : AgentConfiguration {
    override val agentMetadata: AgentMetadata
    override val parameters: AgentParameters
}
