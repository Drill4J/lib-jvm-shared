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


abstract class DummyAgentPart(override val id: String, payload: PluginPayload) : AgentPart<Any, Any>(payload) {

    override fun initPlugin() {
        println("[JAVA SIDE] Plugin $id loaded")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        println("[JAVA SIDE] Plugin '$id' unloaded")
    }

    override fun on() {
        println("[JAVA SIDE] Plugin $id enabled")
    }


    override fun off() {
        println("[JAVA SIDE] Plugin $id disabled")
    }

    override fun updateRawConfig(configs: String) {
        println("update stub")
        //empty
    }

    @Suppress("UNUSED_PARAMETER")
    override val confSerializer: KSerializer<Any>
        get() = TODO("stub")
}