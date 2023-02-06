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

import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

class NativePluginApi(
    val pluginId: String,
    val jvmti: CPointer<jvmtiEnvVar>?,
    val jvm: CPointer<JavaVMVar>?,
    val clb: CPointer<jvmtiEventCallbacks>?,
    val sender: CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>
)

@SharedImmutable
val natContex = Worker.start(true)

@ThreadLocal
var api: NativePluginApi? = null

@ThreadLocal
var plugin: NativePart<*>? = null


inline fun <reified T> pluginApi(noinline what: NativePluginApi.() -> T) =
    natContex.execute(TransferMode.UNSAFE, { what }) {
        it(api!!)
    }.result
