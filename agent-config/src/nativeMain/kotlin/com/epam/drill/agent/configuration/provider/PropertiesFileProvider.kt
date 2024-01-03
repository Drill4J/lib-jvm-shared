/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.configuration.provider

import kotlinx.cinterop.memScoped
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultAgentConfiguration
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open

class PropertiesFileProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = 200
) : AgentConfigurationProvider {

    private val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"
    private val defaultPath = ".${pathSeparator}drill.properties"

    override val configuration: Map<String, String> = configPath()
        .runCatching(::readFile)
        .getOrNull()
        ?.map(String::trim)
        ?.filter { !it.startsWith("#") }
        ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        ?: emptyMap()

    private fun configPath() = fromProviders()
        ?: fromInstallationDir()
        ?: defaultPath

    private fun fromProviders() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultAgentConfiguration.CONFIG_PATH.name] }
        .lastOrNull()

    private fun fromInstallationDir() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultAgentConfiguration.INSTALLATION_DIR.name] }
        .lastOrNull()
        ?.let { "${it}${pathSeparator}drill.properties" }

    private fun readFile(filepath: String): List<String> = memScoped{
        val file = open(filepath, O_RDONLY)
        Input(file).readText().lines().also { close(file) }
    }

}
