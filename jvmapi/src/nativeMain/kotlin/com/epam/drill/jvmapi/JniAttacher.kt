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
package com.epam.drill.jvmapi

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlinx.cinterop.*
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.JNINativeInterface_
import com.epam.drill.jvmapi.gen.JNI_VERSION_1_6
import com.epam.drill.jvmapi.gen.JavaVMVar
import com.epam.drill.jvmapi.gen.jvmtiEnvVar

typealias JNIEnvPointer = CPointer<JNIEnvVar>
typealias JNI = JNINativeInterface_

val vmGlobal = AtomicReference<CPointer<JavaVMVar>?>(null).freeze()
val jvmti = AtomicReference<CPointer<jvmtiEnvVar>?>(null).freeze()

@ThreadLocal
var ex: JNIEnvPointer? = null

val env: JNIEnvPointer
    get() = ex ?: memScoped {
        val vms = vmGlobal.value!!
        val vmFns = vms.pointed.value!!.pointed
        val jvmtiEnvPtr = alloc<CPointerVar<JNIEnvVar>>()
        vmFns.AttachCurrentThread!!(vms, jvmtiEnvPtr.ptr.reinterpret(), null)
        val value: CPointer<CPointerVarOf<JNIEnv>>? = jvmtiEnvPtr.value
        ex = value
        JNI_VERSION_1_6
        value!!
    }

val jni: JNI
    get() = env.pointed.pointed!!

@Suppress("UNUSED")
@CName("getJvm")
fun getJvm(): CPointer<JavaVMVar>? = vmGlobal.value

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer = env

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? = jvmti.value
