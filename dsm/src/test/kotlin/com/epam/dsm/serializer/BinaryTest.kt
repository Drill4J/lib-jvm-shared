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
import com.epam.dsm.common.*
import com.epam.dsm.test.*
import com.epam.dsm.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.jetbrains.exposed.sql.transactions.*
import kotlin.random.Random
import kotlin.test.*

class BinaryTest : PostgresBased(schema) {

    companion object {
        private const val schema: String = "binary_test"
    }

    @Test
    fun `should store and retrieve binary data`() = runBlocking {
        val id = "someIDhere"
        val any = BynariaData(id, byteArrayOf(1, 0, 1),"test")
        storeClient.store(any)
        assertEquals(any.byteArray.contentToString(), storeClient.findById<BynariaData>(id)?.byteArray?.contentToString())
    }

    @Test
    fun `should store and retrieve binary data steam`() {
        transaction {
            createBinaryTable()
        }
        val id = "id"
        val binary = byteArrayOf(-48, -94, -47, -117, 32, -48, -65, -48, -72, -48, -76, -48, -66, -47, -128, 33, 33)
        transaction {
            storeBinary(id, binary, "test")
        }
        val actual = transaction {
            getBinaryAsStream(id)
        }.readBytes()

        assertEquals(binary.contentToString(), actual.contentToString())
    }

    @Test
    fun `should store and retrieve binary data in two differ schema`() = runBlocking {
        val id = "someIDhere"
        val any = BynariaData(id, byteArrayOf(1, 0, 1), "test")
        storeClient.store(any)
        assertEquals(any.byteArray.contentToString(), storeClient.findById<BynariaData>(id)?.byteArray?.contentToString())

        val newDbName = "newdb"
        val newDb = StoreClient(TestDatabaseContainer.createConfig(schema = newDbName))

        newDb.store(any)
        assertEquals(any.byteArray.contentToString(), storeClient.findById<BynariaData>(id)?.byteArray?.contentToString())
        assertEquals(any.byteArray.contentToString(), newDb.findById<BynariaData>(id)?.byteArray?.contentToString())

        storeClient.store(any.copy(id = "2"))

        assertEquals(2, storeClient.getAll<BynariaData>().size)
        assertEquals(1, newDb.getAll<BynariaData>().size)
    }


    @Test
    fun `store classBytes`() = runBlocking {
        val data = CodeData("ID", (1..1000).map { Random.nextBytes(200) })
        storeClient.store(data)
        val storedData = storeClient.findById<CodeData>("ID")
        assertNotNull(storedData)
        data.classBytes.forEach { bytes ->
            assertNotNull(storedData.classBytes.find { bytes.contentToString() == it.contentToString() })
        }
    }

    @Test
    fun `should override object with byte array`() = runBlocking {
        val data = CodeData("ID", (1..20).map { Random.nextBytes(200) })
        storeClient.store(data)
        val storedData = storeClient.findById<CodeData>("ID")
        assertNotNull(storedData)
        assertTrue { storedData.classBytes.size == 20 }
        storeClient.store(data.copy(classBytes = (1..10).map { Random.nextBytes(200) }))
        val overrideStoredData = storeClient.findById<CodeData>("ID")
        assertNotNull(overrideStoredData)
        assertTrue { overrideStoredData.classBytes.size == 10 }
    }

    @Serializable
    data class CodeData(
        @Id
        val id: String,
        val classBytes: List<ByteArray>,
    )

}
