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
import kotlinx.coroutines.*


actual abstract class AgentPart<T, A> actual constructor(payload: PluginPayload) : DrillPlugin<A>, Switchable, Lifecycle {
    private var rawConfig: String? = null

    val config: T get() = confSerializer parse rawConfig!!

    //TODO figure out how to handle suspend from the agent
    fun doRawActionBlocking(rawAction: String) = runBlocking<Unit> {
        doRawAction(rawAction)
    }

    actual open fun init(nativePluginPartPath: String) {
        try {
            System.load(nativePluginPartPath)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    actual fun load(onImmediately: Boolean) {
        initPlugin()
        if (onImmediately)
            on()
    }

    actual fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    actual abstract override fun on()

    actual abstract override fun off()

    external fun loadNative(ss: Long)
    actual var np: NativePart<T>? = null

    open fun updateRawConfig(configs: String) {
        rawConfig = configs
    }

    actual abstract val confSerializer: kotlinx.serialization.KSerializer<T>
    actual abstract override fun initPlugin()

    actual abstract override fun destroyPlugin(unloadReason: UnloadReason)
    actual fun rawConfig(): String {
        return confSerializer stringify config!!
    }

    actual var enabled: Boolean = false
}