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
import com.epam.drill.common.agent.configuration.AgentParameters

class DefaultAgentParameters(
    private val inputParameters: Map<String, String>,
    private val definedParameters: MutableMap<String, Any> = mutableMapOf()
) : AgentParameters {

    private val definitions = mutableMapOf<String, AgentParameterDefinition<out Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(name: String): T = definedParameters[name]!! as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        if(!definedParameters.containsKey(definition.name)) define(definition)
        return definedParameters[definition.name]!! as T
    }

    override fun define(definition: AgentParameterDefinition<out Any>) {
        definitions[definition.name] = definition
        definedParameters[definition.name] = inputParameters[definition.name]
            .also(definition.validator)
            ?.runCatching(definition.parser)
            ?.getOrNull()
            ?: definition.defaultValue
    }

}
