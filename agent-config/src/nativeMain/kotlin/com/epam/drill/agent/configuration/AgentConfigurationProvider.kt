package com.epam.drill.agent.configuration

interface AgentConfigurationProvider {
    val priority: Int
    val configuration: Map<String, String>
}
