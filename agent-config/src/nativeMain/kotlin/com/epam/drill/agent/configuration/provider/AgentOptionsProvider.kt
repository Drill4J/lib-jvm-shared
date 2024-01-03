package com.epam.drill.agent.configuration.provider

import com.epam.drill.agent.configuration.AgentConfigurationProvider

class AgentOptionsProvider(
    agentOptions: String,
    override val priority: Int = 400
) : AgentConfigurationProvider {

    override val configuration = agentOptions.split(",")
        .associate { it.substringBefore("=") to it.substringAfter("=", "") }

}
