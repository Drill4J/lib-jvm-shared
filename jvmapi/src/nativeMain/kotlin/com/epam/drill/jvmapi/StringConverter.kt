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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import com.epam.drill.jvmapi.gen.jstring
import kotlinx.cinterop.ExperimentalForeignApi

class StringConverter {

    @OptIn(ExperimentalForeignApi::class)
    private val jni = env.pointed.pointed!!
    @OptIn(ExperimentalForeignApi::class)
    private val strings = mutableMapOf<jstring, CPointer<ByteVar>?>()

    @OptIn(ExperimentalForeignApi::class)
    fun toKString(jstring: jstring) = jni.GetStringUTFChars!!(env, jstring, null)
        .also { strings[jstring] = it }
        ?.toKString()!!

    @OptIn(ExperimentalForeignApi::class)
    fun release() = strings.forEach { jni.ReleaseStringUTFChars!!(env, it.key, it.value) }

}
