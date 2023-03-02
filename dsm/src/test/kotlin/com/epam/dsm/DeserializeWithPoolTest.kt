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
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlin.test.*

class DeserializeWithPoolTest : PostgresBased("object_pool") {
    private val simpleObjects = (0 until 1000).map {
        SimpleObject("same_id", "same_value", 0, Last(0))
    }

    @Test
    fun `with annotation - should have same references`(): Unit = runBlocking {
        val id = "some_id"
        storeClient.store(ListWithEqualsObjects(id, simpleObjects))
        val stored = storeClient.findById<ListWithEqualsObjects>(id)
        assertNotNull(stored)
        for (i in 0..stored.fromPool.size - 2) {
            assertTrue { stored.fromPool[i] === stored.fromPool[i + 1] }
        }
    }

    @Test
    fun `without annotation - shouldn't have same references`(): Unit = runBlocking {
        val id = "some_id"
        storeClient.store(ListWithEqualsObjects(id, notFromPool = simpleObjects))
        val stored = storeClient.findById<ListWithEqualsObjects>(id)
        assertNotNull(stored)
        for (i in 0..stored.notFromPool.size - 2) {
            assertTrue { stored.notFromPool[i] !== stored.notFromPool[i + 1] }
        }
    }

    @Test
    fun `different objects - should have same references`(): Unit = runBlocking {
        val simpleObject = SimpleObject("id", "string", 123, Last(0))
        storeClient.store(ObjectInPool("id", simpleObject))
        storeClient.store(ObjectInPool("second_id", simpleObject))
        val first = storeClient.findById<ObjectInPool>("id")
        val second = storeClient.findById<ObjectInPool>("second_id")
        assertNotNull(first)
        assertNotNull(second)
        assertTrue { first.simple === second.simple }
    }
}

@Serializable
data class ListWithEqualsObjects(
    @Id val id: String,
    @DeserializeWithPool val fromPool: List<SimpleObject> = emptyList(),
    val notFromPool: List<SimpleObject> = emptyList()
)

@Serializable
data class ObjectInPool(
    @Id val id: String,
    @DeserializeWithPool val simple: SimpleObject
)
