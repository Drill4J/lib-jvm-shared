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

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlin.reflect.KProperty
import com.epam.drill.common.agent.configuration.AgentParameterDefinition
import com.epam.drill.common.agent.configuration.AgentParameters

actual class DefaultAgentParameters actual constructor(
    private val inputParameters: Map<String, String>
) : AgentParameters {

    private val definedParameters = AtomicReference(mapOf<String, Any>())
    private val parameterDefinitions = AtomicReference(mapOf<String, AgentParameterDefinition<out Any>>())

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(name: String): T =
        definedParameters.value[name]!! as T

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        if (!parameterDefinitions.value.containsKey(definition.name)) define(definition)
        return definedParameters.value[definition.name]!! as T
    }

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T =
        definedParameters.value[property.name]!! as T

    actual override fun define(vararg definitions: AgentParameterDefinition<out Any>) {
        val updatedDefinitions = parameterDefinitions.value.toMutableMap()
        val updatedParameters = definedParameters.value.toMutableMap()
        definitions.forEach {
            if (updatedDefinitions.containsKey(it.name)) return@forEach
            updatedDefinitions[it.name] = it
            updatedParameters[it.name] = inputParameters[it.name]
                .also(it.validator)
                ?.runCatching(it.parser)
                ?.getOrNull()
                ?: it.defaultValue
        }
        parameterDefinitions.value = updatedDefinitions.freeze()
        definedParameters.value = updatedParameters.freeze()
    }

}
