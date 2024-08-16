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
@file:Suppress("unused")

package com.epam.drill.agent.instrument

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlinx.cinterop.*
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.jvmapi.checkEx
import com.epam.drill.jvmapi.env
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.jvmapi.jvmti
import com.epam.drill.jvmapi.vmGlobal
import com.epam.drill.logging.LoggingConfiguration

@SharedImmutable
val runtimeJarPath = AtomicReference("")

@Suppress("unused_parameter")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): Int = memScoped {
    vmGlobal.value = vmPointer.freeze()
    val vm = vmPointer.pointed
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    jvmti.value = jvmtiEnvPtr.value.freeze()

    LoggingConfiguration.readDefaultConfiguration()
    LoggingConfiguration.setLoggingLevels("TRACE")
    LoggingConfiguration.setLogMessageLimit(524288)

    val jvmtiCapabilities = alloc<jvmtiCapabilities>()
    jvmtiCapabilities.can_retransform_classes = 1.toUInt()
    jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
    AddCapabilities(jvmtiCapabilities.ptr)

    val alloc = alloc<jvmtiEventCallbacks>()
    alloc.VMInit = staticCFunction(::vmInitEvent)
    alloc.ClassFileLoadHook = staticCFunction(::classFileLoadHook)
    SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)

    runtimeJarPath.value = options.freeze()
    AddToBootstrapClassLoaderSearch(runtimeJarPath.value)

    JNI_OK
}

@Suppress("unused_parameter")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

    callObjectVoidMethod(LoggingConfiguration::class, LoggingConfiguration::readDefaultConfiguration)
    callObjectVoidMethodWithString(LoggingConfiguration::class, "setLoggingLevels", "WARN;com.epam.drill=TRACE")
    callObjectVoidMethodWithInt(LoggingConfiguration::class, LoggingConfiguration::setLogMessageLimit, 524288)
}

@Suppress("unused_parameter")
fun classFileLoadHook(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    clsName: CPointer<ByteVar>?,
    protectionDomain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?,
) = ClassFileLoadHook.invoke(
    loader,
    clsName,
    protectionDomain,
    classDataLen,
    classData,
    newDataLen,
    newData,
)

@CName("Java_com_epam_drill_agent_instrument_TestClassPathProvider_getClassPath")
fun getClassPath(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, TestClassPathProvider::getClassPath)

@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String) = checkEx(errCode, funName)

@CName("currentEnvs")
fun currentEnvs() = env

@CName("jvmtii")
fun jvmtii() = jvmti.value

@CName("getJvm")
fun getJvm() = vmGlobal.value
