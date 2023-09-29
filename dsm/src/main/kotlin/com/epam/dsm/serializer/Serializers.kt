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
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*
import java.util.*
import java.util.stream.*
import kotlin.reflect.*

val DSM_PUSH_LIMIT = System.getenv("DSM_PUSH_LIMIT")?.toIntOrNull() ?: 3_000
val DSM_FETCH_LIMIT = System.getenv("DSM_FETCH_LIMIT")?.toIntOrNull() ?: 10_000

@Serializable
data class BynariaData(
    @Id
    val id: String = uuid,
    @Suppress("ArrayInDataClass")
    val byteArray: ByteArray,
    val agentKey: String?
)

object BinarySerializer : KSerializer<BynariaData> {

    override fun serialize(encoder: Encoder, value: BynariaData) {
        val id = uuid
        transaction {
            val schema = connection.schema
            runBlocking {
                createTableIfNotExists<Any>(schema) {
                    createBinaryTable()
                }
            }
            logger.trace { "serialize for id '$id' in schema $schema" }
            storeBinary(id, value.byteArray, value.agentKey)
        }
        encoder.encodeSerializableValue(String.serializer(), id)
    }

    override fun deserialize(decoder: Decoder): BynariaData {
        val id = decoder.decodeSerializableValue(String.serializer())
        return transaction { getBinary(id) }
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("Binary")
}

/**
 * This serializer stores Collections, in the future Map and ByteArray in a separate table
 * Hashcode of parent object field with annotation @Id is used as the id.
 *
 * Custom serialization for BitSet when storing: Bitsets are serialized into a string of 0's and 1's.
 */
class DsmSerializer<T>(
    private val serializer: KSerializer<T>,
    val classLoader: ClassLoader,
    val parentId: String? = null,
) : KSerializer<T> by serializer {
    override fun serialize(encoder: Encoder, value: T) {
        if (serializer.isBitSet()) {
            encoder.encodeSerializableValue(String.serializer(), (value as BitSet).stringRepresentation())
        } else {
            serializer.serialize(DsmEncoder(encoder), value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        if (serializer.isBitSet()) {
            val decodeSerializableValue = decoder.decodeSerializableValue(String.serializer())
            @Suppress("UNCHECKED_CAST")
            return decodeSerializableValue.toBitSet() as T
        }
        return serializer.deserialize(DsmDecoder(decoder))
    }

    inner class DsmEncoder(private val encoder: Encoder) : Encoder by encoder {

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            val compositeEncoder = encoder.beginStructure(descriptor)
            return DsmCompositeEncoder(compositeEncoder)
        }

        inner class DsmCompositeEncoder(
            private val compositeEncoder: CompositeEncoder,
        ) : CompositeEncoder by compositeEncoder {

            @Suppress("UNCHECKED_CAST")
            override fun <T> encodeSerializableElement(
                descriptor: SerialDescriptor,
                index: Int,
                serializer: SerializationStrategy<T>,
                value: T,
            ) {
                when (value) {
                    is ByteArray, is Enum<*> -> {
                        //TODO EPMDJ-9885 Get rid of the ByteArraySerializer
                        compositeEncoder.encodeSerializableElement(descriptor, index, serializer, value)
                    }
                    is Map<*, *> -> {
                        val mapLikeSerializer = serializer as MapLikeSerializer<*, *, *, *>
                        val keyDescriptor = mapLikeSerializer.keySerializer.descriptor
                        val valueDescriptor = mapLikeSerializer.valueSerializer.descriptor

                        if (!keyDescriptor.isPrimitiveKind() || !valueDescriptor.isPrimitiveKind()) {
                            val clazz = classLoader.run { getClass(keyDescriptor) to getClass(valueDescriptor) }
                            val entrySerializer = mapLikeSerializer.run {
                                keySerializer to valueSerializer
                            } as EntrySerializer<Any, Any>
                            val ids = storeMap(value, parentId, clazz, entrySerializer)
                            compositeEncoder.encodeSerializableElement(
                                descriptor,
                                index,
                                ListSerializer(String.serializer()),
                                ids
                            )
                        } else compositeEncoder.encodeSerializableElement(
                            descriptor,
                            index,
                            serializer as KSerializer<T>,
                            value
                        )
                    }
                    is Collection<*> -> {
                        val elementDescriptor = serializer.descriptor.getElementDescriptor(0)
                        if (elementDescriptor.isPrimitiveKind()) {
                            return compositeEncoder.encodeSerializableElement(descriptor, index, serializer, value)
                        }
                        val ids = if (elementDescriptor.isCollectionElementType(ByteArray::class)) {
                            storeBinaryCollection(unchecked(value.filterNotNull()))
                        } else {
                            val clazz = classLoader.getClass(elementDescriptor)
                            val elementSerializer = clazz.dsmSerializer(parentId, classLoader)
                            storeCollection(value, parentId, clazz, elementSerializer)
                        }
                        return compositeEncoder.encodeSerializableElement(
                            descriptor,
                            index,
                            ListSerializer(String.serializer()),
                            ids
                        )
                    }
                    else -> {
                        val strategy = DsmSerializer(serializer as KSerializer<T>, classLoader, parentId)
                        compositeEncoder.encodeSerializableElement(descriptor, index, strategy, value)
                    }
                }
            }
        }
    }

    inner class DsmDecoder(private val decoder: Decoder) : Decoder by decoder {

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return DsmCompositeDecoder(decoder.beginStructure(descriptor))
        }

        inner class DsmCompositeDecoder(
            private val compositeDecoder: CompositeDecoder,
        ) : CompositeDecoder by compositeDecoder {

            @Suppress("UNCHECKED_CAST")
            override fun <T> decodeSerializableElement(
                descriptor: SerialDescriptor,
                index: Int,
                deserializer: DeserializationStrategy<T>,
                previousValue: T?,
            ): T {
                val registeredPoolSerializers = registeredPoolSerializers.takeIf {
                    descriptor.annotatedWithPool(index)
                }
                return when (deserializer) {
                    ByteArraySerializer() -> {
                        compositeDecoder.decodeSerializableElement(descriptor, index, deserializer)
                    }
                    is MapLikeSerializer<*, *, *, *> -> {
                        val keyDescriptor = deserializer.keySerializer.descriptor
                        val valueDescriptor = deserializer.valueSerializer.descriptor
                        if (!keyDescriptor.isPrimitiveKind() || !valueDescriptor.isPrimitiveKind()) {
                            val ids = decoder.decodeSerializableValue(ListSerializer(String.serializer()))

                            val clazz = classLoader.run { getClass(keyDescriptor) to getClass(valueDescriptor) }
                            val entrySerializer = deserializer.run {
                                keySerializer to registeredPoolSerializers.getOrPutIfNotNull(valueSerializer.descriptor.serialName) { valueSerializer }
                            } as EntrySerializer<Any, Any>
                            val map = loadMap(ids, clazz, entrySerializer)
                            unchecked(map)
                        } else compositeDecoder.decodeSerializableElement(
                            descriptor,
                            index,
                            deserializer as KSerializer<T>
                        )
                    }
                    is AbstractCollectionSerializer<*, *, *> -> {
                        val elementDescriptor = deserializer.descriptor.getElementDescriptor(0)
                        if (elementDescriptor.isPrimitiveKind()) {
                            return compositeDecoder.decodeSerializableElement(
                                descriptor,
                                index,
                                deserializer as KSerializer<T>
                            )
                        }
                        val ids = decoder.decodeSerializableValue(ListSerializer(String.serializer()))
                        if (elementDescriptor.isCollectionElementType(ByteArray::class)) {
                            unchecked(getBinaryCollection(ids).parseCollection(deserializer,
                                ByteArray::class.serializer()))
                        } else {
                            val elementClass = classLoader.getClass(elementDescriptor)
                            val kSerializer = elementClass.dsmSerializer(parentId, classLoader).let {
                                registeredPoolSerializers.getOrPutIfNotNull(it.descriptor.serialName) { it }
                            } as KSerializer<Any>
                            val list = loadCollection(ids, elementClass, kSerializer)
                            unchecked(list.parseCollection(deserializer, elementClass.serializer()))
                        }
                    }
                    else -> {
                        val strategy = registeredPoolSerializers.getOrPutIfNotNull(deserializer.descriptor.serialName) {
                            DsmSerializer(deserializer as KSerializer<T>, classLoader, parentId)
                        }
                        compositeDecoder.decodeSerializableElement(descriptor, index, strategy) as T
                    }
                }
            }
        }
    }

    private fun getClass(elementDescriptor: SerialDescriptor, classLoader: ClassLoader): KClass<Any> {
        @Suppress("UNCHECKED_CAST")
        return classLoader.loadClass(elementDescriptor.serialName).kotlin as KClass<Any>
    }

}

private fun Iterable<Any>.parseCollection(
    des: AbstractCollectionSerializer<*, *, *>,
    serializer2: KSerializer<out Any>,
): Any = when (des::class) {
    ListSerializer(serializer2)::class -> toMutableList()
    SetSerializer(serializer2)::class -> toMutableSet()
    else -> TODO("not implemented yet")
}

inline fun <reified T : Any> dsmDecode(
    inputStream: InputStream,
    classLoader: ClassLoader = T::class.java.classLoader!!,
): T = json.decodeFromStream(
    T::class.dsmSerializer(classLoader = classLoader),
    inputStream
)

inline fun <reified T : Any> dsmDecode(
    inputJson: String,
    classLoader: ClassLoader,
): T = json.decodeFromString(T::class.dsmSerializer(classLoader = classLoader), inputJson)

inline fun <reified T : Any> classLoader(): ClassLoader = T::class.java.classLoader!!

