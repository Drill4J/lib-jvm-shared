package com.epam.drill.agent.configuration

actual object AgentProcessMetadata {
    actual val commandLine: String
        get() = throw NotImplementedError()
    actual val environmentVars: Map<String, String>
        get() = throw NotImplementedError()
}
