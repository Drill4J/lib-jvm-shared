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
package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.*
import kotlinx.serialization.*

abstract class PluginRepresenter(payload: PluginPayload) : AgentPart<Any, Any>(payload) {
    override val serDe: SerDe<Any>
        get() = TODO()
    @Suppress("UNUSED_PARAMETER")
    override val confSerializer: KSerializer<Any>
        get() = TODO()


    override fun initPlugin() = TODO()

    override fun destroyPlugin(unloadReason: UnloadReason) = TODO()

    override suspend fun doAction(action: Any) = TODO()
}