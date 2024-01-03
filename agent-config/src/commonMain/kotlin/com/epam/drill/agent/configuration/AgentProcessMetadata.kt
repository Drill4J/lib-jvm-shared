package com.epam.drill.agent.configuration

expect object AgentProcessMetadata {
    val commandLine: String
    val environmentVars: Map<String, String>
}
