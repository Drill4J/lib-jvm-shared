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

import kotlinx.cinterop.*
import com.epam.drill.jvmapi.gen.*

fun jbyteArray?.readBytes() = this?.let { jbytes ->
    val length = GetArrayLength(jbytes)
    if (length == 0) return@let byteArrayOf()
    val buffer: COpaquePointer? = GetPrimitiveArrayCritical(jbytes, null)
    try {
        ByteArray(length).apply {
            usePinned { destination ->
                platform.posix.memcpy(
                    destination.addressOf(0),
                    buffer,
                    length.convert()
                )
            }
        }
    } finally {
        ReleasePrimitiveArrayCritical(jbytes, buffer, JNI_ABORT)
    }

}

inline fun <reified R> withJString(block: JStringConverter.() -> R): R {
    val jStringConverter = JStringConverter()
    try {
        return block(jStringConverter)
    } finally {
        jStringConverter.localStrings.forEach { (x, y) ->
            jni.ReleaseStringUTFChars!!(env, x, y)
        }
    }
}

class JStringConverter {
    val localStrings = mutableMapOf<jstring, CPointer<ByteVar>?>()
    fun jstring.toKString(): String {
        val nativeString = jni.GetStringUTFChars!!(env, this, null)
        localStrings[this] = nativeString
        return nativeString?.toKString()!!
    }
}

fun jclass.signature(): String = memScoped {
    val ptrVar = alloc<CPointerVar<ByteVar>>()
    GetClassSignature(this@signature, ptrVar.ptr, null)
    ptrVar.value!!.toKString()
}

fun jclass.status(): UInt = memScoped {
    val alloc = alloc<jintVar>()
    GetClassStatus(this@status, alloc.ptr)
    alloc.value.toUInt()
}
