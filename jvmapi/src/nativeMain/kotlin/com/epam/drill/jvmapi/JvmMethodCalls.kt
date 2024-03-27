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

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlinx.cinterop.toKString
import com.epam.drill.jvmapi.gen.CallIntMethod
import com.epam.drill.jvmapi.gen.CallObjectMethod
import com.epam.drill.jvmapi.gen.CallVoidMethod
import com.epam.drill.jvmapi.gen.GetStringUTFChars
import com.epam.drill.jvmapi.gen.NewStringUTF

fun callObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectVoidMethod(clazz, method).run {
        CallVoidMethod(this.first, this.second)
    }

fun callObjectVoidMethod(clazz: KClass<out Any>, method: KCallable<Unit>) =
    callObjectVoidMethod(clazz, method.name)

fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: String, bool: Boolean) =
    getObjectVoidMethodWithBoolean(clazz, method).run {
        CallVoidMethod(this.first, this.second, bool)
    }

fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: KCallable<Unit>, bool: Boolean) =
    callObjectVoidMethodWithBoolean(clazz, method.name, bool)

fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: String, int: Int) =
    getObjectVoidMethodWithInt(clazz, method).run {
        CallVoidMethod(this.first, this.second, int)
    }

fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: KCallable<Unit>, int: Int) =
    callObjectVoidMethodWithInt(clazz, method.name, int)

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectVoidMethodWithString(clazz, method).run {
        CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: KCallable<Unit>, string: String?) =
    callObjectVoidMethodWithString(clazz, method.name, string)

fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: String, bytes: ByteArray) =
    getObjectVoidMethodWithByteArray(clazz, method).run {
        CallVoidMethod(this.first, this.second, toJByteArray(bytes))
    }

fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: KCallable<Unit>, bytes: ByteArray) =
    callObjectVoidMethodWithByteArray(clazz, method.name, bytes)

fun callObjectIntMethod(clazz: KClass<out Any>, method: String) =
    getObjectIntMethod(clazz, method).run {
        CallIntMethod(this.first, this.second)
    }

fun callObjectIntMethod(clazz: KClass<out Any>, method: KCallable<Int>) =
    callObjectIntMethod(clazz, method.name)

fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectObjectMethodWithString(clazz, method).run {
        CallObjectMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

@Suppress("unused")
fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: KCallable<Any?>, string: String?) =
    callObjectObjectMethodWithString(clazz, method.name, string)

fun callObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectStringMethod(clazz, method).run {
        CallObjectMethod(this.first, this.second)?.let { GetStringUTFChars(it, null)?.toKString() }
    }

fun callObjectStringMethod(clazz: KClass<out Any>, method: KCallable<String?>) =
    callObjectStringMethod(clazz, method.name)

fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: String) =
    getObjectByteArrayMethod(clazz, method).run {
        CallObjectMethod(this.first, this.second)?.let(::toByteArray)
    }

fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: KCallable<ByteArray?>) =
    callObjectByteArrayMethod(clazz, method.name)
