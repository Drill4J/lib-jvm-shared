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

class NullableAgentParameterDefinition<T : Any>(
    val name: String,
    val type: KClass<T>,
    val parser: (String) -> T,
    val validator: (String?) -> Unit
) {

    companion object {

        inline fun <reified T : Any> forType(
            name: String,
            noinline parser: (String) -> T,
            noinline validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            T::class,
            parser,
            validator
        )

        fun forString(
            name: String,
            parser: (String) -> String = { it },
            validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            String::class,
            parser,
            validator
        )

        fun forBoolean(
            name: String,
            parser: (String) -> Boolean = { it.toBoolean() },
            validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            Boolean::class,
            parser,
            validator
        )

        fun forInt(
            name: String,
            parser: (String) -> Int = { it.toInt() },
            validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            Int::class,
            parser,
            validator
        )

        fun forLong(
            name: String,
            parser: (String) -> Long = { it.toLong() },
            validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            Long::class,
            parser,
            validator
        )

        fun forDuration(
            name: String,
            parser: (String) -> Duration = { it.toLong().toDuration(DurationUnit.MILLISECONDS) },
            validator: (String?) -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name,
            Duration::class,
            parser,
            validator
        )

    }

}
