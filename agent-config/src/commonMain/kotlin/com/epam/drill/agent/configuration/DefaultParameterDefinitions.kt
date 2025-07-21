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

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition

object DefaultParameterDefinitions {

    val GROUP_ID = NullableAgentParameterDefinition.forString(name = "groupId")
    val APP_ID = NullableAgentParameterDefinition.forString(name = "appId")
    val BUILD_VERSION = NullableAgentParameterDefinition.forString(name = "buildVersion")
    val COMMIT_SHA = NullableAgentParameterDefinition.forString(name = "commitSha")
    val INSTANCE_ID = NullableAgentParameterDefinition.forString(name = "instanceId")
    val ENV_ID = NullableAgentParameterDefinition.forString(name = "envId")
    val PACKAGE_PREFIXES = AgentParameterDefinition.forType(
        name = "packagePrefixes",
        parser = { it.split(";") },
        defaultValue = emptyList()
    )
    val INSTALLATION_DIR = NullableAgentParameterDefinition.forString(name = "drillInstallationDir")
    val CONFIG_PATH = NullableAgentParameterDefinition.forString(name = "configPath")

}
