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
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.test.*

class StoreMapTest : PostgresBased("map") {
    val tableName = "map_test"

    @Test
    fun `should store object with map`(): Unit = runBlocking {
        val id = "some-id"
        val data = (1..100).associate { i ->
            val classId = "classid$i"
            classId to Data(classId, "className$i", "testName$i")
        }
        val map = ObjectWithMap(id, data)
        storeClient.store(map)
        val stored = storeClient.findById<ObjectWithMap>(id)
        assertNotNull(stored)
        assertTrue { stored.data.any() }
        map.data.forEach { (key, value) ->
            val actual = stored.data[key]
            assertNotNull(actual)
            assertTrue { actual == value }
        }
    }

    @Test
    fun `should store object with map with cyrillic symbols`(): Unit = runBlocking {
        val id = "some-id"
        val data = (1..2).associate { i ->
            val classId = "класс$i"
            classId to Data(classId, "имя класса$i", "имя теста $i")
        }
        val map = ObjectWithMap(id, data)
        storeClient.store(map)
        val stored = storeClient.findById<ObjectWithMap>(id)
        assertNotNull(stored)
        assertTrue { stored.data.any() }
        map.data.forEach { (key, value) ->
            val actual = stored.data[key]
            assertNotNull(actual)
            assertTrue { actual == value }
        }
    }

    @Test
    fun `should store map with list values`(): Unit = runBlocking {
        val id = "some-id"
        val data: Map<String, List<Data>> = (1..100).map { i ->
            Data("classid$i", "className$i", "testName$i")
        }.chunked(10).associateBy { "${UUID.randomUUID()}" }
        val objWithMap = ClassWithListValue(id, data)
        storeClient.store(objWithMap)
        val stored = storeClient.findById<ClassWithListValue>(id)
        assertNotNull(stored)
        assertTrue { stored.map.any() }
        objWithMap.map.forEach { (key, value) ->
            val actual = stored.map[key]
            assertNotNull(actual)
            assertTrue { actual.containsAll(value) }
        }
    }

    @Test
    fun `should store map with set values`(): Unit = runBlocking {
        val id = "some-id"
        val data: Map<String, Set<Data>> = (1..100).map { i ->
            Data("classid$i", "className$i", "testName$i")
        }.chunked(10).associate { "${UUID.randomUUID()}" to it.toSet() }
        val objWithMap = ClassWithSetValue(id, data)
        storeClient.store(objWithMap)
        val stored = storeClient.findById<ClassWithSetValue>(id)
        assertNotNull(stored)
        assertTrue { stored.map.any() }
        objWithMap.map.forEach { (key, value) ->
            val actual = stored.map[key]
            assertNotNull(actual)
            assertTrue { actual.containsAll(value) }
        }
    }

    @Test
    fun `should store object with empty map`(): Unit = runBlocking {
        val id = "some-id"
        val map = ObjectWithMap(id, emptyMap())
        storeClient.store(map)
        val stored = storeClient.findById<ObjectWithMap>(id)
        assertNotNull(stored)
    }

    @Test
    fun `should be no conflict - map with inner list`(): Unit = runBlocking {
        val id = "some-id"
        val data = (1..1000).map { i ->
            Data("classid$i", "className$i", "testName$i")
        }.chunked(2).associate {
            val objectsWithList = it.chunked(2).map { data ->
                ObjectWithList(uuid, data)
            }
            val objectWithInnerList = ObjectWithInnerList(uuid, objectsWithList)
            uuid to objectWithInnerList
        }
        val mapWithInnerList = MapWithInnerList(id, data)
        storeClient.store(mapWithInnerList)
        val stored = storeClient.findById<MapWithInnerList>(id)
        assertNotNull(stored)
        mapWithInnerList.map.entries.forEach { (key, value) ->
            val actual = stored.map[key]
            assertNotNull(actual)
            assertTrue { actual.data.containsAll(value.data) }
        }
    }

    @Test
    fun `should override object with empty map`(): Unit = runBlocking {
        val id = "some-id"
        val map = ObjectWithMap(id, emptyMap())
        storeClient.store(map)
        val stored = storeClient.findById<ObjectWithMap>(id)
        assertNotNull(stored)
        storeClient.store(map)
        val storedAgain = storeClient.findById<ObjectWithMap>(id)
        assertNotNull(storedAgain)
    }
}

@Serializable
data class ObjectWithMap(
    @Id val id: String,
    val data: Map<String, Data>,
)

@Serializable
data class ClassWithListValue(
    @Id val id: String,
    val map: Map<String, List<Data>>,
)

@Serializable
data class ClassWithSetValue(
    @Id val id: String,
    val map: Map<String, Set<Data>>,
)

@Serializable
data class MapWithInnerList(
    @Id val id: String,
    val map: Map<String, ObjectWithInnerList>,
)
