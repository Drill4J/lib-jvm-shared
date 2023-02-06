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


import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

inline fun <reified T : Any> instance(): Pair<jclass?, jobject?> {
    val name = T::class.qualifiedName!!.replace(".", "/")
    return instance(name)
}

fun instance(name: String): Pair<jclass?, jobject?> {
    val requestHolderClass = FindClass(name)
    val selfMethodId: jfieldID? = GetStaticFieldID(requestHolderClass, "INSTANCE", "L$name;")
    val requestHolder: jobject? = GetStaticObjectField(requestHolderClass, selfMethodId)
    return Pair(requestHolderClass, requestHolder)
}

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

inline fun <reified R> withJSting(block: JStingConverter.() -> R): R {
    val jStingConverter = JStingConverter()
    try {
        return block(jStingConverter)
    } finally {
        jStingConverter.localStrings.forEach { (x, y) ->
            jni.ReleaseStringUTFChars!!(env, x, y)
        }
    }
}

class JStingConverter {
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

@Suppress("NOTHING_TO_INLINE")
inline fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.sequenceOf(count: Int): Sequence<T> {
    var current = 0
    return generateSequence<T> {
        if (current == count) null
        else this[current++]
    }
}

inline fun jbyteArray.toByteArrayWithRelease(
    bytes: ByteArray = byteArrayOf(),
    block: (ByteArray) -> Unit
) {
    val nativeArray = GetByteArrayElements(this, null)?.apply {
        bytes.forEachIndexed { index, byte -> this[index] = byte }
    }
    try {
        val length = GetArrayLength(this)
        block(ByteArray(length).apply {
            usePinned { destination ->
                platform.posix.memcpy(
                    destination.addressOf(0),
                    nativeArray,
                    length.convert()
                )
            }
        })
    } finally {
        ReleaseByteArrayElements(this, nativeArray, JNI_ABORT)
    }
}
