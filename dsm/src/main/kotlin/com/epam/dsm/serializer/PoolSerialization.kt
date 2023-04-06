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
package com.epam.dsm.serializer

import com.epam.dsm.*
import com.epam.dsm.util.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.lang.ref.*
import java.util.*
import java.util.concurrent.*

typealias PoolSerializers = ConcurrentHashMap<String, KSerializer<*>>

internal val registeredPoolSerializers: PoolSerializers = ConcurrentHashMap()

class PoolSerializer<T>(private val serializer: KSerializer<T>) : KSerializer<T> by serializer {

    private val weakRefObjectPool = WeakHashMap<T, WeakReference<T>>()

    private fun T.weakIntern(): T {
        val cached = weakRefObjectPool[this]
        if (cached != null) {
            val value = cached.get()
            if (value != null) return value
        }
        weakRefObjectPool[this] = WeakReference(this)
        return this
    }

    override fun deserialize(decoder: Decoder): T {
        return serializer.deserialize(decoder).weakIntern()
    }
}

fun PoolSerializers?.getOrPutIfNotNull(key: String, default: () -> KSerializer<*>) = this?.getOrPut(key) {
    PoolSerializer(default())
} ?: default()

fun SerialDescriptor.annotatedWithPool(index: Int) = findAnnotation<DeserializeWithPool>() == getElementName(index)
