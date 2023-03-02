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
import com.epam.dsm.test.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.exceptions.*
import org.junit.jupiter.api.*
import kotlin.test.*
import kotlin.test.Test

class InitStoreClientTest : PostgresBased("public") {

    private val last = Last(2.toByte())
    private val simpleObject = SimpleObject("id", "subStr", 12, last)

    @Test
    fun `should create one public connection`() = runBlocking {
        storeClient.store(simpleObject)
        assertEquals(simpleObject, storeClient.getAll<SimpleObject>().first())
    }

    @Test
    fun `should create new schema by config and crete connect pool`() = runBlocking {
        val another = StoreClient(hikariConfig = TestDatabaseContainer.createConfig(schema = "another"))

        another.store(simpleObject)
        assertEquals(simpleObject, another.getAll<SimpleObject>().first())
        storeClient.store(simpleObject)
        storeClient.store(simpleObject.copy(id = "id2"))

        assertEquals(1, another.getAll<SimpleObject>().size)
        assertEquals(2, storeClient.getAll<SimpleObject>().size)
    }

    @RepeatedTest(value = 15)
    fun `should close pool connection when use test data source`() {
        StoreClient(hikariConfig = TestDatabaseContainer.createDataSource(schema = "another"))
    }

    @Test
    fun `should be exception when schema is empty`() = runBlocking {
        assertThrows<ExposedSQLException> {
            StoreClient(hikariConfig = TestDatabaseContainer.createConfig(schema = ""))
        }
        Unit
    }
}

