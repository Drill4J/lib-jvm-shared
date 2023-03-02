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
import com.epam.dsm.common.PrepareData.Companion.setPayload
import com.epam.dsm.util.*
import kotlinx.serialization.*
import kotlin.test.*
import kotlin.test.Test

class DsmSerializerTest {
    private val jsonPayload = """
              {
                "id": "third",
                "subObject": {
                  "int": 12,
                  "last": {
                    "string": 2
                  },
                  "string": "subStr"
                },
                "nameExample": "test1"
              }
        """.trimIndent()

    @Test
    fun `should deserialize object`() {
        assertEquals(setPayload, json.decodeFromString(jsonPayload))

        assertEquals(setPayload, dsmDecode(jsonPayload, classLoader<SetPayload>()))
    }

    @Test
    fun `should deserialize list of objects`() {
        val input = "[$jsonPayload]"
        val expected: List<SetPayload> = listOf(setPayload)
        //using listSerializer:
        assertEquals(expected, json.decodeFromString<List<SetPayload>>(input))
        //using AbstractPolymorphicSerializer:
        assertEquals(expected, dsmDecode(input, classLoader<SetPayload>()))
    }

}

