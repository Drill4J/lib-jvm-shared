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

import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlinx.cinterop.toKString
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import com.epam.drill.jvmapi.gen.*

fun callObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()V").run {
        CallVoidMethod(this.first, this.second)
    }

fun callObjectVoidMethod(clazz: KClass<out Any>, method: KCallable<Unit>) =
    callObjectVoidMethod(clazz, method.name)

fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: String, bool: Boolean) =
    getObjectMethod(clazz, method, "(Z)V").run {
        CallVoidMethod(this.first, this.second, bool)
    }

fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: KCallable<Unit>, bool: Boolean) =
    callObjectVoidMethodWithBoolean(clazz, method.name, bool)

fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: String, int: Int) =
    getObjectMethod(clazz, method, "(I)V").run {
        CallVoidMethod(this.first, this.second, int)
    }

fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: KCallable<Unit>, int: Int) =
    callObjectVoidMethodWithInt(clazz, method.name, int)

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)V").run {
        CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: KCallable<Unit>, string: String?) =
    callObjectVoidMethodWithString(clazz, method.name, string)

fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: String, bytes: ByteArray) =
    getObjectMethod(clazz, method, "([B)V").run {
        CallVoidMethod(this.first, this.second, toJByteArray(bytes))
    }

fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: KCallable<Unit>, bytes: ByteArray) =
    callObjectVoidMethodWithByteArray(clazz, method.name, bytes)

fun callObjectIntMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()I").run {
        CallIntMethod(this.first, this.second)
    }

fun callObjectIntMethod(clazz: KClass<out Any>, method: KCallable<Int>) =
    callObjectIntMethod(clazz, method.name)

fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)Ljava/lang/Object;").run {
        CallObjectMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: KCallable<Any?>, string: String?) =
    callObjectObjectMethodWithString(clazz, method.name, string)

fun callObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()Ljava/lang/String;").run {
        CallObjectMethod(this.first, this.second)?.let { GetStringUTFChars(it, null)?.toKString() }
    }

fun callObjectStringMethod(clazz: KClass<out Any>, method: KCallable<String?>) =
    callObjectStringMethod(clazz, method.name)

fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()[B").run {
        CallObjectMethod(this.first, this.second)?.let(::toByteArray)
    }

fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: KCallable<ByteArray?>) =
    callObjectByteArrayMethod(clazz, method.name)

fun getObjectMethod(clazz: KClass<out Any>, method: String, signature: String) = run {
    val className = clazz.qualifiedName!!.replace(".", "/")
    val classRef = FindClass(className)
    val methodId = GetMethodID(classRef, method, signature)
    val instanceId = GetStaticFieldID(classRef, "INSTANCE", "L$className;")
    val instaceRef = GetStaticObjectField(classRef, instanceId)
    instaceRef to methodId
}

fun toJByteArray(array: ByteArray) = NewByteArray(array.size)!!.apply {
    array.usePinned { SetByteArrayRegion(this, 0, array.size, it.addressOf(0)) }
}

fun toByteArray(jarray: jobject) = ByteArray(GetArrayLength(jarray)).apply {
    if (this.isEmpty()) return@apply
    val buffer = GetPrimitiveArrayCritical(jarray, null)
    try {
        this.usePinned { memcpy(it.addressOf(0), buffer, this.size.convert()) }
    } finally {
        ReleasePrimitiveArrayCritical(jarray, buffer, JNI_ABORT)
    }
}
