package com.epam.drill.agent.configuration.provider

import kotlinx.cinterop.toKString
import platform.posix.getenv
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.AgentProcessMetadata
import com.epam.drill.agent.configuration.DefaultAgentConfiguration

class InstallationDirProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = 100
) : AgentConfigurationProvider {

    private val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

    override val configuration: Map<String, String>
        get() = mapOf(Pair(DefaultAgentConfiguration.INSTALLATION_DIR.name, installationDir()))

    private fun installationDir() = fromProviders()
        ?: fromJavaToolOptions()
        ?: fromCommandLine()
        ?: "."

    private fun fromProviders() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultAgentConfiguration.INSTALLATION_DIR.name] }
        .lastOrNull()

    private fun fromJavaToolOptions() = getenv("JAVA_TOOL_OPTIONS")?.toKString()
        ?.substringAfter("-agentpath:")
        ?.substringBefore("=")
        ?.substringBeforeLast(pathSeparator)

    private fun fromCommandLine() = runCatching(AgentProcessMetadata::commandLine::get).getOrNull()
        ?.substringAfter("-agentpath:")
        ?.substringBefore("=")
        ?.substringBeforeLast(pathSeparator)

}
