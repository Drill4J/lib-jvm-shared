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

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import kotlinx.serialization.*

expect abstract class AgentPart<T, A>(payload: PluginPayload) : DrillPlugin<A>, Switchable, Lifecycle {
    var np: NativePart<T>?
    var enabled: Boolean

    abstract val confSerializer: KSerializer<T>

    open fun init(nativePluginPartPath: String)

    fun load(onImmediately: Boolean)
    fun unload(unloadReason: UnloadReason)


    abstract override fun initPlugin()

    abstract override fun destroyPlugin(unloadReason: UnloadReason)

    abstract override fun on()

    abstract override fun off()

    fun rawConfig(): String
}


interface Switchable {
    fun on()
    fun off()
}

interface Lifecycle {
    fun initPlugin()
    fun destroyPlugin(unloadReason: UnloadReason)
}

expect abstract class NativePart<T> {
    actual abstract val confSerializer: KSerializer<T>
    fun updateRawConfig(config: PluginConfig)
}

enum class UnloadReason {
    ACTION_FROM_ADMIN, SH
}