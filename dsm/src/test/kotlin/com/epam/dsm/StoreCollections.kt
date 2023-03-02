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

import com.epam.dsm.common.*
import com.epam.dsm.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.junit.jupiter.api.*
import kotlin.test.*
import kotlin.test.Test

class StoreCollections : PostgresBased("store_collections") {

    @Test
    fun `should store object with list`(): Unit = runBlocking {
        val id = uuid
        val countOfElements = 10
        val data = (0 until countOfElements).map {
            Data("classID$it", "className$it", "testName$it")
        }
        val objWithList = ObjectWithList(id, data)
        storeClient.store(objWithList)
        val storedObject = storeClient.findById<ObjectWithList>(id)
        assertNotNull(storedObject)
        assertTrue { storedObject.data.isNotEmpty() }
        assertTrue { storedObject.data.size == countOfElements }
    }

    @Test
    fun `should store object with empty list`() {
        val id = uuid
        val objWithList = ObjectWithList(id, emptyList())
        assertDoesNotThrow { runBlocking { storeClient.store(objWithList) } }
        assertDoesNotThrow { runBlocking { storeClient.findById<ObjectWithList>(id) } }
    }

    @Test
    fun `should store element of collection with reserved symbols`(): Unit = runBlocking {
        val id = uuid
        val testName =
            "Can't store this test name \\b \\f \\n \\r \\t \\7 \\85h \\u88 U&'d\\0061t\\+000061' \$\$Dianne's horse\$\$ + - * / < > = ~ ! @ # % ^ & | ` ?"
        val data = Data("classID", "className", testName)
        val objWithList = ObjectWithList(id, listOf(data))
        assertDoesNotThrow {
            runBlocking { storeClient.store(objWithList) }
        }
        val storedObject = storeClient.findById<ObjectWithList>(id)
        assertNotNull(storedObject)
        assertEquals(testName, storedObject.data.first().testName)
    }

    @Test
    fun `should store element of collection with cyrillic symbols`(): Unit = runBlocking {
        val id = uuid
        val data = (0..2).map {
            Data("класс $it", "имя класса $it", "имя теста $it")
        }
        val objWithList = ObjectWithList(id, data)
        assertDoesNotThrow {
            runBlocking { storeClient.store(objWithList) }
        }
        val storedObject = storeClient.findById<ObjectWithList>(id)
        assertNotNull(storedObject)
    }

    @Test
    fun `should store object with inner lists`(): Unit = runBlocking {
        val data = (0..10).map {
            Data("classID$it", "className$it", "testName$it")
        }
        val objWithList = ObjectWithList("id", data)
        val objWithInnerList = ObjectWithInnerList("someId", listOf(objWithList))
        storeClient.store(objWithInnerList)
        val stored = storeClient.findById<ObjectWithInnerList>("someId")
        assertNotNull(stored)
        assertTrue { stored.data.any() }
        assertTrue { stored.data.all { it.data.any() } }
    }

    @Test
    fun `store collection with cyrillic letters`(): Unit = runBlocking {
        val data = Data("класс", "имяКласса", "имяТеста")
        val id = "someId"
        val objWithList = ObjectWithList(id, listOf(data))
        storeClient.store(objWithList)
        val storedObject = storeClient.findById<ObjectWithList>(id)
        assertNotNull(storedObject)
        assertTrue { storedObject.data.any() }
        assertTrue { storedObject.data.first().classId == "класс" }
    }

    @Test
    fun `should be no conflict - two lists`(): Unit = runBlocking {
        val id = "id"
        val data = (0..100).map {
            Data("classID$it", "className$it", "testName$it")
        }
        val objectWithList = ObjectWithList(id, data.subList(0, 50), data.subList(50, 100))
        storeClient.store(objectWithList)
        val stored = storeClient.findById<ObjectWithList>(id)
        assertNotNull(stored)
        assertTrue { stored.data.containsAll(objectWithList.data) }
        assertTrue { stored.anotherData.containsAll(objectWithList.anotherData) }
    }

    @Test
    fun `should delete by id`(): Unit = runBlocking {
        val id = "id"
        storeClient.store(ObjectWithList(id, listOf(Data("classID", "className", "testName"))))
        storeClient.deleteById<ObjectWithList>(id)
        assertEquals(emptyList(), storeClient.getAll<ObjectWithList>())
        assertEquals(emptyList(), storeClient.getAll<Data>())
    }

    @Test
    fun `should delete by query`(): Unit = runBlocking {
        val id = "id"
        storeClient.store(ObjectWithList(id, listOf(Data("classID", "className", "testName"))))
        storeClient.deleteBy<ObjectWithList> {
            ObjectWithList::id eq id
        }
        assertEquals(emptyList(), storeClient.getAll<ObjectWithList>())
        assertEquals(emptyList(), storeClient.getAll<Data>())
    }

    @Test
    fun `should update with the same id`(): Unit = runBlocking {
        val id = "id"
        val element = Data("classID", "className", "testName")
        storeClient.store(ObjectWithList(id, listOf(element)))
        val updateData = listOf(element.copy(classId = "another"))
        storeClient.store(ObjectWithList(id, updateData))
        assertEquals(ObjectWithList(id, updateData), storeClient.getAll<ObjectWithList>().first())
        assertEquals(1, storeClient.getAll<Data>().size)
    }

    @Test
    fun `should cascade delete collection for same id`(): Unit = runBlocking {
        val id = "id"
        val data = (0 until 100).map {
            Data("classID$it", "className$it", "testName$it")
        }
        val objectWithList = ObjectWithList(id, data)
        storeClient.store(objectWithList)
        assertTrue { storeClient.getAll<Data>().size == 100 }

        val overrideObjectWithList = ObjectWithList(id, emptyList())
        storeClient.store(overrideObjectWithList)
        assertTrue { storeClient.getAll<Data>().isEmpty() }

    }


}

@Serializable
data class ObjectWithInnerList(
    @Id val id: String,
    val data: List<ObjectWithList>,
)

@Serializable
data class ObjectWithList(
    @Id val id: String,
    val data: List<Data>,
    val anotherData: List<Data> = emptyList(),
)

@Serializable
data class Data(
    val classId: String,
    val className: String,
    val testName: String,
)
