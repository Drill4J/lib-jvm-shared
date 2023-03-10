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
package com.epam.drill.agent.runner

open class AppAgentConfiguration : Configuration() {
    override val repositoryName: String = "java-agent"

    var buildVersion: String? = null
    var instanceId: String? = null
    var webAppNames: List<String>? = null

    override fun jvmArgs(): Map<String, String> {
        val args = mutableMapOf<String, String>()
        buildVersion?.let { args[AppAgentConfiguration::buildVersion.name] = it }
        instanceId?.let { args[AppAgentConfiguration::instanceId.name] = it }
        webAppNames?.let { args[AppAgentConfiguration::webAppNames.name] = it.joinToString(separator = ":") }
        return args
    }

}

