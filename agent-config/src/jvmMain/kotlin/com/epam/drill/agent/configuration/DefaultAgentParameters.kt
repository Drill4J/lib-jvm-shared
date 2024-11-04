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

import kotlin.reflect.KProperty
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameters

actual class DefaultAgentParameters actual constructor(
    private val inputParameters: Map<String, String>
) : AgentParameters {

    private val definedParameters = mutableMapOf<String, Any>()
    private val parameterDefinitions = mutableMapOf<String, AgentParameterDefinition<out Any>>()

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(name: String): T =
        definedParameters[name]!! as T

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        if (!parameterDefinitions.containsKey(definition.name)) define(definition)
        return definedParameters[definition.name]!! as T
    }

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T =
        definedParameters[property.name]!! as T

    actual override fun define(vararg definitions: AgentParameterDefinition<out Any>) {
        definitions.forEach {
            if (parameterDefinitions.containsKey(it.name)) return@forEach
            parameterDefinitions[it.name] = it
            definedParameters[it.name] = inputParameters[it.name]
                .also(it.validator)
                ?.runCatching(it.parser)
                ?.getOrNull()
                ?: it.defaultValue
        }
    }

}
