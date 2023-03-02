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
@file:Suppress("BlockingMethodInNonBlockingContext")

package com.epam.dsm.find

import com.epam.dsm.*
import com.epam.dsm.EnumExample.*
import com.epam.dsm.common.*
import com.epam.dsm.common.PrepareData.Companion.storeLists
import com.epam.dsm.find.Expr.Companion.ANY
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.test.*
import kotlin.test.Test

const val DELIMITER_PATH = "->"

class ExpressionTest : PostgresBased("expression") {
    private val last = Last(2.toByte())
    private val blink = SubObject("subStr", 12, last)
    private val complexObject = ComplexObject("str", 'x', blink, SECOND, null)
    private val simpleObject = SimpleObject("id", "subStr", 12, last)

    @Test
    fun `should delete object with startsWith expression`() = runBlocking {
        storeClient.store(simpleObject)
        storeClient.deleteBy<SimpleObject> {
            SimpleObject::id startsWith "i"
        }
        assertTrue(storeClient.getAll<SimpleObject>().isEmpty())
    }

    @Test
    fun `should delete object with like expression`() = runBlocking {
        storeClient.store(simpleObject)
        storeClient.deleteBy<SimpleObject> {
            SimpleObject::string like "sub${ANY}tr"
        }
        assertTrue(storeClient.getAll<SimpleObject>().isEmpty())
    }

    @Test
    fun `should find object with complex expressions`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))
        val all = storeClient.findBy<ComplexObject> {
            (ComplexObject::enumExample eq SECOND) and (ComplexObject::id eq "123")
        }
        assertTrue(all.get().isNotEmpty())
    }

    @Serializable
    data class DefaultExample(
        @Id val id: String,
        val num: Int = 0,
        val str: String = "something",
        val list: List<String> = emptyList(),
        val enum: EnumExample = FIRST,
        val subDefault: SubDefault = SubDefault(),
    )

    @Serializable
    class SubDefault {
        val subFields: String = "defaultValue"
    }

    @Test
    fun `should delete when field has default value`() = runBlocking {
        storeClient.store(DefaultExample("example"))
        storeClient.deleteBy<DefaultExample> {
            DefaultExample::str eq "something"
        }
        assertTrue(storeClient.getAll<DefaultExample>().isEmpty())
    }

    @Test
    fun `should delete when field has default value in inner field`() = runBlocking {
        storeClient.store(DefaultExample("example"))
        storeClient.deleteBy<DefaultExample> {
            FieldPath(DefaultExample::subDefault, SubDefault::subFields) eq "defaultValue"
        }
        assertTrue(storeClient.getAll<DefaultExample>().isEmpty())
    }

    @Test
    fun `should find objects when build expression with default value`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", enumExample = FIRST))
        assertEquals(2, storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::enumExample) containsWithNull listOf("SECOND", "FIRST")
        }.get().size)
    }

    @Test
    fun `should find objects when use 'OR' operation`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))

        assertEquals(2, storeClient.findBy<ComplexObject> {
            (ComplexObject::id eq "123") or (ComplexObject::id eq "str")
        }.get().size)
    }


    @Test
    fun `should find objects when use 'in'`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))
        assertEquals(2, storeClient.findBy<ComplexObject> {
            ComplexObject::id contains listOf("123", "str")
        }.get().size)
    }

    @Test
    fun `should find objects when build expression`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))
        assertEquals(2, storeClient.findBy<ComplexObject> {
            ComplexObject::id contains listOf("123", "str")
        }.get().size)
    }

    @Test
    fun `should find by inner field when use eq by object`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))

        assertEquals(2, storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::blink.name, SubObject::string.name) eq "subStr"
        }.get().size)

        assertEquals(2, storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::blink, SubObject::string) eq "subStr"
        }.get().size)
    }

    @Test
    fun `should find by inner field when input has string`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))
        val field = ComplexObject::blink.name
        val field2 = SubObject::string.name
        val input = "$field$DELIMITER_PATH$field2"
        val findBy = storeClient.findBy<ComplexObject> {
            FieldPath(input.split(DELIMITER_PATH)) eq "subStr"
        }
        assertEquals(2, findBy.get().size)
    }

    @Test
    fun `should find by inner field when use 'and' operation`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123", ch = 'w'))
        val findBy = storeClient.findBy<ComplexObject> {
            ComplexObject::id eq "str" and (FieldPath(ComplexObject::blink, SubObject::string) eq "subStr")
        }
        assertEquals(1, findBy.get().size)
    }

    @Test
    fun `should find only one whe use distinct`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(id = "123"))
        val findBy = storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::ch) eq "x"
        }.distinct()
        val result = findBy.getAndMap(ComplexObject::enumExample)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find when find by default and mapping`() = runBlocking {
        storeClient.store(complexObject.copy(id = "123", enumExample = FIRST))
        val findBy = storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::ch) eq "x"
        }
        val result = findBy.getAndMap(ComplexObject::enumExample)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find when property can be null`() = runBlocking {
        storeClient.store(complexObject.copy(id = "123", enumExample = FIRST))
        val findBy = storeClient.findBy<ComplexObject> {
            (ComplexObject::ch eq "x")
        }
        val result = findBy.getAndMap(ComplexObject::enumExample)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find when use equals with case insensitive`() = runBlocking {
        storeClient.store(complexObject.copy(id = "case", ch = 't'))
        val findBy = storeClient.findBy<ComplexObject> {
            FieldPath(ComplexObject::ch) eqIgnoreCase "T"
        }
        assertEquals(1, findBy.get().size)
        assertEquals(1,
            storeClient.findBy<ComplexObject> {
                (ComplexObject::id eqIgnoreCase "CASE")
            }.get().size
        )

    }

    @Test
    fun `should find nothing when list table does not create yet`() = runBlocking {
        val findBy = storeClient.findBy<SetPayload> {
            containsParentId(listOf("23"))
        }
        assertEquals(0, findBy.get().size)
        assertEquals(0, findBy.getAndMap(SetPayload::nameExample).size)
    }

    @Test
    fun `should find when search in list of subjects `() = runBlocking {
        storeClient.storeLists()
        val findBy = storeClient.findBy<SetPayload> {
            FieldPath(SetPayload::id) eq "second"
        }
        assertEquals(1, findBy.get().size)
        assertEquals(1, findBy.getAndMap(SetPayload::id).size)

    }


}



