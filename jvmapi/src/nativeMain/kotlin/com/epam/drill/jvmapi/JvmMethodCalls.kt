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

import com.epam.drill.jvmapi.gen.CallObjectMethod
import kotlin.reflect.KClass
import com.epam.drill.jvmapi.gen.CallVoidMethod
import com.epam.drill.jvmapi.gen.FindClass
import com.epam.drill.jvmapi.gen.GetMethodID
import com.epam.drill.jvmapi.gen.GetStaticFieldID
import com.epam.drill.jvmapi.gen.GetStaticObjectField
import com.epam.drill.jvmapi.gen.GetStringUTFChars
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.gen.ReleaseStringUTFChars
import kotlinx.cinterop.toKString
import kotlin.reflect.KCallable

fun callObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()V").run {
        CallVoidMethod(this.first, this.second)
    }

fun callObjectVoidMethod(clazz: KClass<out Any>, method: KCallable<Unit>) =
    callObjectVoidMethod(clazz, method.name)

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)V").run {
        CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: KCallable<Unit>, string: String?) =
    callObjectVoidMethodWithString(clazz, method.name, string)

fun callObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()Ljava/lang/String;").run {
        CallObjectMethod(this.first, this.second)?.let { GetStringUTFChars(it, null)?.toKString() }
    }

fun callObjectStringMethod(clazz: KClass<out Any>, method: KCallable<String?>) =
    callObjectStringMethod(clazz, method.name)

private fun getObjectMethod(clazz: KClass<out Any>, method: String, signature: String) = run {
    val className = clazz.qualifiedName!!.replace(".", "/")
    val classRef = FindClass(className)
    val methodId = GetMethodID(classRef, method, signature)
    val instanceId = GetStaticFieldID(classRef, "INSTANCE", "L$className;")
    val instaceRef = GetStaticObjectField(classRef, instanceId)
    instaceRef to methodId
}
