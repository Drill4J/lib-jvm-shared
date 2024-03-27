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
import com.epam.drill.jvmapi.gen.FindClass
import com.epam.drill.jvmapi.gen.GetMethodID
import com.epam.drill.jvmapi.gen.GetStaticFieldID
import com.epam.drill.jvmapi.gen.GetStaticObjectField

fun getObjectMethod(clazz: KClass<out Any>, method: String, signature: String) = run {
    val className = clazz.qualifiedName!!.replace(".", "/")
    val classRef = FindClass(className)
    val methodId = GetMethodID(classRef, method, signature)
    val instanceId = GetStaticFieldID(classRef, "INSTANCE", "L$className;")
    val instaceRef = GetStaticObjectField(classRef, instanceId)
    instaceRef to methodId
}

fun getObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()V")

fun getObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Z)V")

fun getObjectVoidMethodWithInt(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(I)V")

fun getObjectVoidMethodWithString(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)V")

fun getObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "([B)V")

fun getObjectIntMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()I")

fun getObjectObjectMethodWithString(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)Ljava/lang/Object;")

fun getObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()Ljava/lang/String;")

fun getObjectByteArrayMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()[B")
