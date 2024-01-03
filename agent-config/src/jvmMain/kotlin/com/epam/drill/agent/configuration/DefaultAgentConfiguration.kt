package com.epam.drill.agent.configuration

import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.configuration.AgentParameters

actual class DefaultAgentConfiguration(
    actual override val agentMetadata: AgentMetadata,
    inputParameters: Map<String, String>,
    definedParameters: MutableMap<String, Any>
) : AgentConfiguration {

    actual override val parameters: AgentParameters = DefaultAgentParameters(inputParameters, definedParameters)

}
