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
package com.epam.drill.agent.common.configuration

import kotlin.reflect.KProperty

interface AgentParameters {
    operator fun <T : Any> get(name: String): T?
    operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T
    operator fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T?
    fun define(vararg definitions: AgentParameterDefinition<out Any>)
    operator fun <T : Any> get(definition: NullableAgentParameterDefinition<T>): T?
    fun define(vararg definitions: NullableAgentParameterDefinition<out Any>)
}
