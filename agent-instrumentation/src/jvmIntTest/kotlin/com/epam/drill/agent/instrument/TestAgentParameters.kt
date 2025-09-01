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
package com.epam.drill.agent.instrument

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.BaseAgentParameterDefinition
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition
import com.epam.drill.agent.common.configuration.ValidationError
import kotlin.reflect.KProperty

object TestAgentParameters: AgentParameters {
    override fun <T : Any> get(name: String): T? {
        throw NotImplementedError("Not implemented")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        return when (definition.type) {
            String::class -> definition.defaultValue ?: "" as T
            Boolean::class -> definition.defaultValue ?: false as T
            Int::class -> definition.defaultValue ?: 0 as T
            else -> throw IllegalArgumentException("No value for parameter '${definition.name}'")
        }
    }

    override fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T? {
        throw NotImplementedError("Not implemented")
    }

    override fun <T : Any> get(definition: NullableAgentParameterDefinition<T>): T? {
        throw NotImplementedError("Not implemented")
    }

    override fun define(vararg definitions: BaseAgentParameterDefinition<*>): List<ValidationError<*>> {
        throw NotImplementedError("Not implemented")
    }
}