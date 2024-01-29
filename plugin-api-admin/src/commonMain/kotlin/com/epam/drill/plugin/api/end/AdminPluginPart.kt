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
package com.epam.drill.plugin.api.end

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*

@Suppress("unused")
abstract class AdminPluginPart<A>(
    val id: String,
    val agentInfo: AgentInfo,
    val adminData: AdminData,
    val sender: Sender
) {
    open suspend fun initialize() = Unit

    abstract suspend fun doAction(action: A, data: Any?): Any

    abstract fun parseAction(rawAction: String): A

    suspend fun doRawAction(rawAction: String, data: Any? = null): Any = doAction(parseAction(rawAction), data)

    open suspend fun processData(
        message: Any,
    ): Any? = null

    open suspend fun applyPackagesChanges() = Unit

    @Deprecated("", replaceWith = ReplaceWith(""))
    open suspend fun dropData() = Unit
}
