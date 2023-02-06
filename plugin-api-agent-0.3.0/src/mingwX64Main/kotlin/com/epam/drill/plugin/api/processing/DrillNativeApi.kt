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
@file:Suppress("FunctionName", "unused")

package com.epam.drill.plugin.api.processing

import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

//todo MOVE IT TO API KLIB:)

@SymbolName("currentThread")
internal external fun currentThread(): jthread?

@SymbolName("jvmtiCallbacks")
internal external fun jvmtiCallback(): jvmtiEventCallbacks?

@SymbolName("SetEventCallbacksP")
internal external fun SetEventCallbacksP(
    callbacks: kotlinx.cinterop.CValuesRef<jvmtiEventCallbacks>?,
    size_of_callbacks: jint /* = kotlin.Int */
)

@SymbolName("enableJvmtiEventExceptionCatch")
internal external fun enableJvmtiEventExceptionCatch(th: jthread?)

@SymbolName("jvmtix")
internal external fun jvmtix(): CPointer<jvmtiEnvVar>?

@SymbolName("sendToSocket")
internal external fun sendToSocket(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>)

@SymbolName("enableJvmtiEventException")
internal external fun enableJvmtiEventException(thread: jthread? = null)

@SymbolName("disableJvmtiEventException")
internal external fun disableJvmtiEventException(thread: jthread? = null)

@SymbolName("addPluginToRegistry")
internal external fun addPluginToRegistry(plugin: NativePart<*>)

@SymbolName("getPlugin")
internal external fun getPlugin(id: CPointer<ByteVar>): NativePart<*>?

