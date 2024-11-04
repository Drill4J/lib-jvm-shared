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

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AgentParameterDefinition<T : Any>(
    val name: String,
    val type: KClass<T>,
    val defaultValue: T,
    val parser: (String) -> T,
    val validator: (String?) -> Unit
) {

    companion object {

        inline fun <reified T : Any> forType(
            name: String,
            defaultValue: T,
            noinline parser: (String) -> T,
            noinline validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            T::class,
            defaultValue,
            parser,
            validator
        )

        fun forString(
            name: String,
            defaultValue: String = "",
            parser: (String) -> String = { it },
            validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            String::class,
            defaultValue,
            parser,
            validator
        )

        fun forBoolean(
            name: String,
            defaultValue: Boolean = false,
            parser: (String) -> Boolean = { it.toBoolean() },
            validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            Boolean::class,
            defaultValue,
            parser,
            validator
        )

        fun forInt(
            name: String,
            defaultValue: Int = 0,
            parser: (String) -> Int = { it.toInt() },
            validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            Int::class,
            defaultValue,
            parser,
            validator
        )

        fun forLong(
            name: String,
            defaultValue: Long = 0L,
            parser: (String) -> Long = { it.toLong() },
            validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            Long::class,
            defaultValue,
            parser,
            validator
        )

        fun forDuration(
            name: String,
            defaultValue: Duration = Duration.ZERO,
            parser: (String) -> Duration = { it.toLong().toDuration(DurationUnit.MILLISECONDS) },
            validator: (String?) -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            Duration::class,
            defaultValue,
            parser,
            validator
        )

    }

}
