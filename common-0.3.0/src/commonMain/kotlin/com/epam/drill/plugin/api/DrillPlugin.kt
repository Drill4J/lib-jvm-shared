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
package com.epam.drill.plugin.api

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*


interface DrillPlugin<A> {
    val id: String

    val serDe: SerDe<A>

    suspend fun doAction(action: A): Any

    suspend fun doRawAction(rawAction: String): Any {
        val action = serDe.actionSerializer.parse(rawAction)
        return doAction(action)
    }
    
    infix fun <T> KSerializer<T>.parse(rawData: String) = serDe.decodeFromString(this, rawData)

    infix fun <T> KSerializer<T>.stringify(rawData: T) = serDe.encodeToString(this, rawData)
}


class SerDe<A>(
    val actionSerializer: KSerializer<A>,
    private val fmt: StringFormat = Json
) : StringFormat by fmt