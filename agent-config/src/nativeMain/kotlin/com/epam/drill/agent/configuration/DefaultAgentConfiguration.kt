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
package com.epam.drill.agent.configuration

import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.configuration.AgentParameterDefinition
import com.epam.drill.common.agent.configuration.AgentParameters
import com.epam.drill.common.agent.configuration.AgentType

actual class DefaultAgentConfiguration(
    private val configurationProviders: Set<AgentConfigurationProvider>
) : AgentConfiguration {

    companion object {
        val AGENT_ID = AgentParameterDefinition.forString(name = "agentId")
        val INSTANCE_ID = AgentParameterDefinition.forString(name = "instanceId")
        val BUILD_VERSION = AgentParameterDefinition.forString(name = "buildVersion")
        val GROUP_ID = AgentParameterDefinition.forString(name = "groupId")
        val AGENT_VERSION = AgentParameterDefinition.forString(name = "agentVersion")
        val PACKAGE_PREFIXES = AgentParameterDefinition.forType(
            name = "packagePrefixes",
            parser = { it.split(";").toList() },
            defaultValue = emptyList()
        )
        val INSTALLATION_DIR = AgentParameterDefinition.forString(name = "drillInstallationDir")
        val CONFIG_PATH = AgentParameterDefinition.forString(name = "configPath")
    }

    actual override val parameters: AgentParameters = parameters()

    actual override val agentMetadata = agentMetadata()

    private fun parameters() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .map(AgentConfigurationProvider::configuration)
        .reduce { acc, map -> acc.plus(map) }
        .let(::DefaultAgentParameters)

    private fun agentMetadata() = AgentMetadata(
        id = parameters[AGENT_ID],
        instanceId = parameters[INSTANCE_ID],
        buildVersion = parameters[BUILD_VERSION],
        serviceGroupId = parameters[GROUP_ID],
        agentType = AgentType.JAVA,
        agentVersion = parameters[AGENT_VERSION],
        packagesPrefixes = parameters[PACKAGE_PREFIXES]
    )

}
