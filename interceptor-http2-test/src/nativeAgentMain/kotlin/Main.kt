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
@file:Suppress("UNUSED_PARAMETER", "unused", "FunctionName")

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.jvmapi.gen.jobject
import kotlin.native.concurrent.freeze
import kotlinx.cinterop.CPointer
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import com.epam.drill.interceptor.*
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.vmGlobal
import com.epam.drill.logging.LoggingConfiguration

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    LoggingConfiguration.readDefaultConfiguration()
    KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.TRACE
    configureHttpInterceptor2()
    com.epam.drill.hook.io.injectedHeaders.value = { mapOf("xxx" to "yyy") }.freeze()
    com.epam.drill.hook.io.readHeaders.value = { it: Map<ByteArray, ByteArray> ->
        it.forEach { (k, v) -> println("${k.decodeToString()}: ${v.decodeToString()}") }
    }.freeze()
    com.epam.drill.hook.io.readCallback.value = { _: ByteArray -> println("READ") }.freeze()
    com.epam.drill.hook.io.writeCallback.value = { _: ByteArray -> println("WRITE") }.freeze()

    return 0
}

@Suppress("unused")
@CName("Java_bindings_Bindings_removeHttpHook")
fun removeHttpHook(env: JNIEnv, thiz: jobject) {
    com.epam.drill.hook.io.removeTcpHook()
}

@Suppress("unused")
@CName("Java_bindings_Bindings_addHttpHook")
fun addHttpHook(env: JNIEnv, thiz: jobject) {
    LoggingConfiguration.readDefaultConfiguration()
    KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.TRACE
    configureHttpInterceptor2()
    com.epam.drill.hook.io.injectedHeaders.value = { mapOf("xxx" to "yyy") }.freeze()
    com.epam.drill.hook.io.readHeaders.value = { it: Map<ByteArray, ByteArray> ->
        it.forEach { (k, v) -> println("${k.decodeToString()}: ${v.decodeToString()}") }
    }.freeze()
    com.epam.drill.hook.io.readCallback.value = { _: ByteArray -> println("READ") }.freeze()
    com.epam.drill.hook.io.writeCallback.value = { _: ByteArray -> println("WRITE") }.freeze()
}

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return com.epam.drill.jvmapi.currentEnvs()
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? {
    return com.epam.drill.jvmapi.jvmtii()
}

@CName("getJvm")
fun getJvm(): CPointer<JavaVMVar>? {
    return vmGlobal.value
}

@CName("JNI_OnUnload")
fun JNI_OnUnload() {
}

@CName("JNI_GetCreatedJavaVMs")
fun JNI_GetCreatedJavaVMs() {
}

@CName("JNI_CreateJavaVM")
fun JNI_CreateJavaVM() {
}

@CName("JNI_GetDefaultJavaVMInitArgs")
fun JNI_GetDefaultJavaVMInitArgs() {
}

@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}
