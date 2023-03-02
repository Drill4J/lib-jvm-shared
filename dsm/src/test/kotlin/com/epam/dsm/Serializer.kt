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
package com.epam.dsm

import com.epam.dsm.util.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.*

//search select bit_or(Cast(JSON_BODY->>'bitset' as BIT VARYING(10000000))) FROM bittest.bitset_class
object BitSetSerializer : KSerializer<BitSet> {

    override fun serialize(encoder: Encoder, value: BitSet) {
        encoder.encodeSerializableValue(String.serializer(), value.stringRepresentation())
    }

    override fun deserialize(decoder: Decoder): BitSet {
        val decodeSerializableValue = decoder.decodeSerializableValue(String.serializer())
        return decodeSerializableValue.toBitSet()
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("BitSet")
}
