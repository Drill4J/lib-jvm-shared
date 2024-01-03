package com.epam.drill.agent.configuration.provider

import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.AgentProcessMetadata

class EnvironmentVariablesProvider(
    override val priority: Int = 500
) : AgentConfigurationProvider {

    override val configuration = runCatching(AgentProcessMetadata::environmentVars::get).getOrNull()
        ?.filterKeys { it.startsWith("DRILL_") }
        ?.mapKeys(::toParameterName)
        ?: emptyMap()

    private fun toParameterName(entry: Map.Entry<String, String>) = entry.key
        .removePrefix("DRILL_")
        .lowercase()
        .split("_")
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)

}
