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

package com.epam.dsm.util

import com.epam.dsm.*
import com.epam.dsm.serializer.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import java.io.*
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

val json = Json {
    allowStructuredMapKeys = true
    encodeDefaults = true
}

inline fun <reified T : Any> T.id(): String {
    val idNames = T::class.serializer().descriptor.idNames()
    val propertiesWithId = T::class.memberProperties.filter { idNames.contains(it.name) }
    val uniqueId = propertiesWithId.map {
        it.getter.invoke(this)?.hashCode()
    }.fold("") { acc, hashCode ->
        acc + hashCode.toString()
    }
    if (uniqueId.isEmpty()) {
        throw RuntimeException("Property with @Id doesn't found")
    }
    return uniqueId
}

fun SerialDescriptor.idNames(): List<String> = (0 until elementsCount).filter { index ->
    getElementAnnotations(index).any { it is Id }
}.map { idIndex -> getElementName(idIndex) }

inline fun <reified A : Annotation> SerialDescriptor.findAnnotation(): String? = (0 until elementsCount).firstOrNull { index ->
    getElementAnnotations(index).any { it is A }
}?.let { idIndex -> getElementName(idIndex) }

fun SerialDescriptor.findColumnAnnotation(
    path: List<String> = emptyList(),
    result: MutableMap<Column, List<String>> = mutableMapOf(),
): Map<Column, List<String>> = (0 until elementsCount).forEach { index ->
    val innerDescriptor = getElementDescriptor(index)
    val element = getElementName(index)
    getElementAnnotations(index).find { it is Column }?.let {
        result[it as Column] = path + element
    }
    if (innerDescriptor.isClassSerialKind()) {
        innerDescriptor.findColumnAnnotation(path + element, result)
    } else return@forEach
}.run { result }

fun Any?.encodeId(): String = this?.run {
    when (this) {
        is String -> this
        is Enum<*> -> toString()
        else -> json.encodeToString(unchecked(this::class.serializer()), this)
    }
} ?: throw RuntimeException("Can not encode null value")

inline fun <reified T : Any> KClass<T>.dsmSerializer(
    parentId: String? = null,
    classLoader: ClassLoader = T::class.java.classLoader!!,
): KSerializer<T> {
    val curSerializer = this.serializer()
    val serializer = if (curSerializer is AbstractPolymorphicSerializer<*>) {
        json.serializersModule.serializer()
    } else curSerializer
    return DsmSerializer(serializer, classLoader, parentId)
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T> unchecked(any: Any) = any as T


@Suppress("NOTHING_TO_INLINE")
inline fun SerialDescriptor.isPrimitiveKind() = kind is PrimitiveKind

@Suppress("NOTHING_TO_INLINE")
inline fun SerialDescriptor.isClassSerialKind() = kind in listOf(
    StructureKind.CLASS, StructureKind.OBJECT, PolymorphicKind.OPEN, SerialKind.CONTEXTUAL
)

@Suppress("NOTHING_TO_INLINE")
inline fun FileOutputStream.size() = channel.size()

@Suppress("NOTHING_TO_INLINE")
internal inline fun KSerializer<*>.isBitSet() = descriptor.serialName == BitSet::class.simpleName

@Suppress("NOTHING_TO_INLINE")
internal inline fun SerialDescriptor.isCollectionElementType(
    kClass: KClass<*>,
) = serialName == kClass.qualifiedName

fun String.toQuotes(): String = "'$this'"
