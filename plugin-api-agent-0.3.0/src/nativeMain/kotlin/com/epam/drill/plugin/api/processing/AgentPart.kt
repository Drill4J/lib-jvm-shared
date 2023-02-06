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

actual abstract class AgentPart<T, A> actual constructor(payload: PluginPayload) : DrillPlugin<A>, Switchable, Lifecycle {

    abstract suspend fun isEnabled(): Boolean

    abstract suspend fun setEnabled(value: Boolean)

    actual open fun init(nativePluginPartPath: String) {
    }

    actual open fun load(onImmediately: Boolean) {
        initPlugin()
        if (onImmediately)
            on()
    }

    actual open fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    actual var np: NativePart<T>? = null

    actual abstract val confSerializer: KSerializer<T>

    abstract fun updateRawConfig(config: PluginConfig)

    actual fun rawConfig(): String {
        return if (np != null)
            np!!.confSerializer stringify np!!.config!!
        else ""
    }

    actual var enabled: Boolean = false

}