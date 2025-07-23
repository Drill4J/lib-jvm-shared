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

import com.epam.drill.agent.konform.validation.Valid
import com.epam.drill.agent.konform.validation.ValidationBuilder
import com.epam.drill.agent.konform.validation.ValidationResult
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class NullableAgentParameterDefinition<T : Any>(
    name: String,
    description: String? = null,
    type: KClass<T>,
    parser: (String) -> T,
    validation: ValidationType = ValidationType.STRICT,
    validator: (T?) -> ValidationResult<*>
) : BaseAgentParameterDefinition<T>(
    name,
    description,
    type,
    parser,
    validation,
    validator
) {

    companion object {

        inline fun <reified T : Any> forType(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            noinline parser: (String) -> T,
            noinline validator: (T?) -> ValidationResult<*> = { Valid(true) }
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = T::class,
            parser = parser,
            validation = validation,
            validator = validator
        )

        fun forString(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> String = { it },
            validator: ValidationBuilder<String>.() -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = String::class,
            parser = parser,
            validation = validation,
            validator = { validateIfPresent(it, validator) }
        )

        fun forBoolean(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Boolean = { it.toBoolean() },
            validator: ValidationBuilder<Boolean>.() -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = Boolean::class,
            parser = parser,
            validation = validation,
            validator = { validateIfPresent(it, validator) }
        )

        fun forInt(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Int = { it.toInt() },
            validator: ValidationBuilder<Int>.() -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = Int::class,
            parser = parser,
            validation = validation,
            validator = { validateIfPresent(it, validator) }
        )

        fun forLong(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Long = { it.toLong() },
            validator: ValidationBuilder<Long>.() -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = Long::class,
            parser = parser,
            validation = validation,
            validator = { validateIfPresent(it, validator) }
        )

        fun forDuration(
            name: String,
            description: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Duration = { it.toLong().toDuration(DurationUnit.MILLISECONDS) },
            validator: ValidationBuilder<Duration>.() -> Unit = {}
        ) = NullableAgentParameterDefinition(
            name = name,
            description = description,
            type = Duration::class,
            parser = parser,
            validation = validation,
            validator = { validateIfPresent(it, validator) }
        )

    }

}
