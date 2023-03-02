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

package com.epam.dsm

import com.epam.dsm.common.*
import com.epam.dsm.find.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.test.*
import kotlin.test.Test

class DsmCoreTest : PostgresBased("plugin") {
    private val last = Last(2.toByte())
    private val blink = SubObject("subStr", 12, last)
    private val complexObject = ComplexObject("str", 'x', blink, EnumExample.SECOND, null)
    private val simpleObject = SimpleObject("id", "subStr", 12, last)

    @Test
    fun `should store and retrieve an object with composite id`() = runBlocking {
        val id = CompositeId("one", 1)
        val data = CompositeData(id, "data")
        storeClient.store(data)
        val all = storeClient.getAll<CompositeData>()
        assertEquals(1, all.count())
        val foundById = storeClient.findById<CompositeData>(CompositeId("one", 1))
        assertEquals(data, foundById)
        val foundByExprWithId = storeClient.findBy<CompositeData> { CompositeData::id eq id }.get()
        assertEquals(data, foundByExprWithId.first())
        val foundByExprWithData = storeClient.findBy<CompositeData> { CompositeData::data eq "data" }.get()
        assertEquals(data, foundByExprWithData.first())
    }

    @Test
    fun `should correctly update an object with composite id`() = runBlocking {
        val id = CompositeId("one", 1)
        updateObject(CompositeData(id, "data"), id, storeClient)
        updateObject(CompositeData(id, "data2"), id, storeClient)
    }

    @Test
    fun `should correctly create,update,delete an object with id and extra one annotation`() = runBlocking {
        val id = CompositeId("one", 1)
        updateObject(ObjectWithTwoAnnotation(id, 100), id, storeClient)
        updateObject(ObjectWithTwoAnnotation(id, 2000), id, storeClient)
        storeClient.deleteById<ObjectWithTwoAnnotation>(id)
        assertEquals(0, storeClient.getAll<ObjectWithTwoAnnotation>().count())
    }

    @Test
    fun `should store and retrieve a simple object`() = runBlocking {
        storeClient.store(simpleObject)
        val all = storeClient.getAll<SimpleObject>()
        val simpleObject1 = all.first()
        assertTrue(all.isNotEmpty())
        assertEquals(simpleObject1.id, "id")
        assertEquals(simpleObject1.int, 12)
        assertEquals(simpleObject1.string, "subStr")
        assertEquals(simpleObject1.last, last)
    }

    @Test
    fun `should store and retrieve a simple object with single quote`() = runBlocking {
        val simpleObject = SimpleObject("id", "string with ' single quote", 1, Last(1))
        storeClient.store(simpleObject)
        val simpleObject1 = storeClient.findById<SimpleObject>(simpleObject.id)
        assertEquals(simpleObject1?.string, simpleObject.string)
    }

    @Test
    fun `should retrieve null when object does not added`() = runBlocking {
        val simpleObject = storeClient.findById<SimpleObject>("12412d")
        assertNull(simpleObject)
    }

    @Test
    fun `should retrieve empty collection when object does not added`() = runBlocking {
        val simpleObject = storeClient.findBy<SimpleObject> { SimpleObject::id eq "1" }.get()
        assertTrue(simpleObject.isEmpty())
    }

    @Test
    fun `should store and retrieve an object with all-default payload`() = runBlocking {
        val withDefaults = ObjectWithDefaults("some-id")
        storeClient.store(withDefaults)
        val all = storeClient.getAll<ObjectWithDefaults>()
        assertEquals(listOf(withDefaults), all)
    }

    @Test
    fun `should store and retrieve a complex object`() = runBlocking {
        storeClient.store(complexObject)
        val all = storeClient.getAll<ComplexObject>()
        val cm = all.first()
        assertTrue(all.isNotEmpty())
        assertEquals(cm.id, "str")
        assertEquals(cm.ch, 'x')
        assertEquals(cm.blink, blink)
        assertEquals(cm.enumExample, EnumExample.SECOND)
        assertNull(cm.nullString)
    }

    @Test
    fun `should store and retrieve an object with complex nesting`() = runBlocking {
        val withDefaults = ComplexListNesting("some-id")
        storeClient.store(withDefaults)
        val all = storeClient.getAll<ComplexListNesting>()
        assertEquals(listOf(withDefaults), all)
    }

    @Test
    fun `should remove entities of a complex object by ID recursively`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.deleteById<ComplexObject>("str")
        assertTrue(storeClient.getAll<ComplexObject>().isEmpty())
    }

    @Test
    fun `should remove entities of a complex object by Prop recursively`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.deleteBy<ComplexObject> { ComplexObject::enumExample eq EnumExample.SECOND }
        assertTrue(storeClient.getAll<ComplexObject>().isEmpty())
    }

    @Test
    fun `should update object`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject.copy(ch = 'y'))
        assertEquals(storeClient.getAll<ComplexObject>().first().ch, 'y')
    }

    @Test
    fun `should store several objects`() = runBlocking {
        storeClient.store(complexObject.copy(id = "1"))
        storeClient.store(complexObject.copy(id = "2"))
        val all = storeClient.getAll<ComplexObject>()
        assertEquals(2, all.size)
    }

    @Test
    fun `should store the same object`() = runBlocking {
        storeClient.store(complexObject)
        storeClient.store(complexObject)
        val all = storeClient.getAll<ComplexObject>()
        assertEquals(1, all.size)
    }

    @Test
    fun `should store and retrieve object with primitive collection`() = runBlocking {
        storeClient.store(ObjectWithPrimitiveElementsCollection(1, listOf("st1", "st2")))
        val all = storeClient.getAll<ObjectWithPrimitiveElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference collection`() = runBlocking {
        storeClient.store(ObjectWithReferenceElementsCollection(1, setOf(TempObject("st", 2))))
        val all = storeClient.getAll<ObjectWithReferenceElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with primitive map`() = runBlocking {
        storeClient.store(ObjectWithPrimitiveElementsMap(1, mapOf("x" to 2)))
        val all = storeClient.getAll<ObjectWithPrimitiveElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with mixed map - Simple key and complex value`() = runBlocking {
        storeClient.store(ObjectWithReferenceElementsMapMixed(1, mapOf("x" to TempObject("stsa", 3))))
        val all = storeClient.getAll<ObjectWithReferenceElementsMapMixed>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference map`() = runBlocking {
        storeClient.store(ObjectWithReferenceElementsMap(3, mapOf(TempObject("st", 2) to TempObject("stsa", 3))))
        val all = storeClient.getAll<ObjectWithReferenceElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }


    @Serializable
    data class ObjectWithList(@Id val id: String, val primitiveList: List<Boolean>)

    @Test
    fun `should restore list related object with right order`() = runBlocking {
        val primitiveList = listOf(true, false, false, true)
        storeClient.store(ObjectWithList("id", primitiveList))
        val actual = storeClient.findById<ObjectWithList>("id")
        assertNotNull(actual)
        actual.primitiveList.forEachIndexed { index, pred ->
            assertEquals(primitiveList[index], pred)
        }
    }

    @Test
    fun `should be transactional with double storing`() = runBlocking {
        try {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            storeClient.executeInAsyncTransaction {
                store(complexObject)
                store(complexObject.copy(id = "second"))
            }
        } catch (ignored: Throwable) {
        }
        assertEquals(2, storeClient.getAll<ComplexObject>().size)
    }

    @Test
    fun `should be transactional`() = runBlocking {
        try {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            storeClient.executeInAsyncTransaction {
                store(complexObject)
                fail("test")
            }
        } catch (ignored: Throwable) {
        }
        assertTrue(storeClient.getAll<ComplexObject>().isEmpty())
    }

    @Test
    fun `should be transactional without schema name`() = runBlocking {
        try {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            storeClient.executeInAsyncTransaction {
                store(complexObject)
                fail("test")
            }
        } catch (ignored: Throwable) {
        }
        assertTrue(storeClient.getAll<ComplexObject>().isEmpty())
    }

    @Test
    fun `should be transactional when delete smth twice`() = runBlocking {
        storeClient.store(complexObject.copy(id = "1"))
        storeClient.store(complexObject.copy(id = "2"))
        try {
            storeClient.executeInAsyncTransaction {
                deleteBy<ComplexObject> { ComplexObject::id eq "1" }
                fail("test")
                @Suppress("UNREACHABLE_CODE")
                deleteBy<ComplexObject> { ComplexObject::id eq "2" }
            }
        } catch (ignored: Throwable) {
            println(ignored)
        }
        assertEquals(2, storeClient.getAll<ComplexObject>().size)
    }

    @Test
    fun `should not store old entities when set field gets updated`() = runBlocking {
        val set = mutableSetOf(
            SetPayload("1", "name1")
        )
        val obj = ObjectWithSetField("myId", set)
        storeClient.store(obj)
        assertEquals(1, storeClient.findById<ObjectWithSetField>("myId")?.set?.count())
        assertEquals("name1", storeClient.findById<ObjectWithSetField>("myId")?.set?.firstOrNull()?.nameExample)
        val storedObj = storeClient.findById<ObjectWithSetField>("myId")!!
        storedObj.set.removeAll { it.id == "1" }
        storedObj.set.add(SetPayload("1", "name2"))
        storeClient.store(storedObj)
        assertEquals(1, storeClient.findById<ObjectWithSetField>("myId")?.set?.count())
        assertEquals("name2", storeClient.findById<ObjectWithSetField>("myId")?.set?.firstOrNull()?.nameExample)
    }

    @Test
    fun `should preserve and restore objects with byte arrays among fields`() = runBlocking {
        val testArray = "test".toByteArray()
        val obj = ObjectWithByteArray("myArray", testArray)
        storeClient.store(obj)
        val retrieved = storeClient.findById<ObjectWithByteArray>("myArray")
        assertTrue(testArray.contentEquals(retrieved?.array!!))
    }

    @Test
    fun `should preserve and retrieve map fields with Enum keys`() = runBlocking {
        val obj = MapField("test", mapOf(EnumExample.FIRST to TempObject("a", 5)))
        storeClient.store(obj)
        val retrievedMap = storeClient.findById<MapField>("test")?.map.orEmpty()
        assertEquals(5, retrievedMap[EnumExample.FIRST]?.int)
    }


    @Test
    fun `should delete all entities for a specified class`() = runBlocking<Unit> {
        val obj1 = StoreMe("id1")
        val obj2 = StoreMe("id2")
        val obj3 = MapField("id3")
        storeClient.store(obj1)
        storeClient.store(obj2)
        storeClient.store(obj3)
        assertEquals(2, storeClient.getAll<StoreMe>().count())
        assertNotNull(storeClient.findById<StoreMe>("id1"))
        assertNotNull(storeClient.findById<StoreMe>("id2"))
        assertNotNull(storeClient.findById<MapField>("id3"))
        storeClient.deleteAll<StoreMe>()
        assertEquals(0, storeClient.getAll<StoreMe>().count())
        assertNull(storeClient.findById<StoreMe>("id1"))
        assertNull(storeClient.findById<StoreMe>("id2"))
        assertNotNull(storeClient.findById<MapField>("id3") != null)
    }

    @Test
    fun `should store and get empty set as default`() = runBlocking {
        val expected = ClassWithSet("id")
        storeClient.store(expected)
        assertEquals(expected, storeClient.findById("id"))
    }

    @Test
    fun `should store and get empty set`() = runBlocking {
        val expected = ClassWithSet("id2", emptySet())
        storeClient.store(expected)
        assertEquals(expected, storeClient.findById("id2"))
    }

    @Test
    fun `should delete by part of composite id`() = runBlocking {
        val id = CompositeId("one", 1)
        val data = CompositeData(id, "data")
        storeClient.store(data)
        storeClient.deleteBy<CompositeData> {
            FieldPath(CompositeData::id, CompositeId::str) eq id.str
        }
        assertNull(storeClient.findById<CompositeData>(id))
    }


}

@Serializable
data class ClassWithSet(
    @Id val id: String,
    val set: Set<AnotherClass> = emptySet(),
)

@Serializable
data class AnotherClass(
    val something: String,
)
