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

import com.epam.drill.common.agent.configuration.AgentParameterDefinition

object DefaultParameterDefinitions {

    val AGENT_ID = AgentParameterDefinition.forString(name = "agentId")
    val INSTANCE_ID = AgentParameterDefinition.forString(name = "instanceId")
    val BUILD_VERSION = AgentParameterDefinition.forString(name = "buildVersion")
    val GROUP_ID = AgentParameterDefinition.forString(name = "groupId")
    val AGENT_VERSION = AgentParameterDefinition.forString(name = "agentVersion")
    val PACKAGE_PREFIXES = AgentParameterDefinition.forType(
        name = "packagePrefixes",
        defaultValue = emptyList(),
        parser = { it.split(";") }
    )
    val INSTALLATION_DIR = AgentParameterDefinition.forString(name = "drillInstallationDir")
    val CONFIG_PATH = AgentParameterDefinition.forString(name = "configPath")

}