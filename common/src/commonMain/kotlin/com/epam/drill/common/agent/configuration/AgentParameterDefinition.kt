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
package com.epam.drill.common.agent.configuration

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

open class AgentParameterDefinition<T> (
    val name: String,
    val type: KClass<out Any>,
    val parser: (String) -> T,
    val validator: (String) -> Unit,
    val defaultValue: T?
) {

    companion object {

        fun forString(
            name: String,
            parser: (String) -> String = { it },
            validator: (String) -> Unit = {},
            defaultValue: String? = null
        ) = AgentParameterDefinition(name, String::class, parser, validator, defaultValue)

        fun forBoolean(
            name: String,
            parser: (String) -> Boolean = { it.toBoolean() },
            validator: (String) -> Unit = {},
            defaultValue: Boolean? = null
        ) = AgentParameterDefinition(name, Boolean::class, parser, validator, defaultValue)

        fun forInt(
            name: String,
            parser: (String) -> Int = { it.toInt() },
            validator: (String) -> Unit = {},
            defaultValue: Int? = null
        ) = AgentParameterDefinition(name, Int::class, parser, validator, defaultValue)

        fun forLong(
            name: String,
            parser: (String) -> Long = { it.toLong() },
            validator: (String) -> Unit = {},
            defaultValue: Long? = null
        ) = AgentParameterDefinition(name, Long::class, parser, validator, defaultValue)

        fun forDuration(
            name: String,
            parser: (String) -> Duration = { it.toLong().toDuration(DurationUnit.MILLISECONDS) },
            validator: (String) -> Unit = {},
            defaultValue: Duration? = null
        ) = AgentParameterDefinition(name, Duration::class, parser, validator, defaultValue)

    }

}
