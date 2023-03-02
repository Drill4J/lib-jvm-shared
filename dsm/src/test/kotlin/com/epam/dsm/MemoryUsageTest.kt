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
import com.epam.dsm.serializer.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random
import kotlin.test.*

/**
 * This test emulating storing and loading session in test2code plugin
 */
class MemoryUsageTest : PostgresBased(schema) {

    companion object {
        private const val schema = "session"
    }

    @Test
    fun `perf test! store and load a lot of data`() = runBlocking {
        val countSessions = 10
        repeat(countSessions) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("store session, index = $it...") }
            startStopSession("sessionId$index")
        }
        repeat(countSessions) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("load session, index = $it...") }
            loadSession("sessionId$index")
        }
    }

    @Test
    fun `perf test! store and load big data`() = runBlocking {
        val sessionId = "sessionId"
        //ok
        startStopSession(sessionId, 1, 1_000, 20_000)
        // exceeds the maximum (256 mb) for jsonb
        //startStopSession(sessionId, 1, 5_000, 20_000)
        loadSession(sessionId)
    }

    private suspend fun loadSession(s: String) {
        println("Loading session")
        val finishSession = storeClient.findById<FinishSession>(s)
        assertTrue((finishSession?.execClassData?.size ?: 0) > 0)
        assertTrue((finishSession?.execClassData?.sumOf { it.probes.size() } ?: 0) > 0)
    }

    private suspend fun startStopSession(
        sessionId: String,
        countAddProbes: Int = 1,
        sizeExec: Int = 1_000,
        sizeProbes: Int = 20_000,
    ) {

        var collection: List<ExecClassData> = mutableListOf()
        repeat(countAddProbes) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("adding probes, index = $it...") }
            val execClassData: List<ExecClassData> = listOf(0 until sizeExec).flatten().map {
                ExecClassData(
                    id = Random.nextLong(100_000_000),
                    className = "foo/Bar",
                    probes = BitSetInit(sizeProbes)
                )
            }
            collection = execClassData
        }

        storeClient.store(FinishSession(sessionId, collection))

    }

    private fun randomBoolean(n: Int = 100) = listOf(0 until n).flatten().map { true }
}

@Serializable
@StreamSerialization
data class FinishSession(
    @Id
    val sessionId: String,
    val execClassData: List<ExecClassData>,
)

@Serializable
data class ExecClassData(
    val id: Long? = null,
    val className: String,
    @Serializable(with = BitSetSerializer::class)
    val probes: BitSet,
    val testName: String = "",
)


/**
 *  OOM With such config with `string memory test` and all ok with `stream memory test`
 *  Tested with heap size = 1g
 */
@Disabled
class StreamSerializationTest : PostgresBased(schema) {

    private val size = 200_000
    private val largeObject = LargeObject(
        "id",
        (1..size).map { "$it".repeat(20) },
        (1..size).associate { "$it".repeat(20) to "$it".repeat(20) },
    )

    private val annotatedLargeObject = LargeObjectWithStreamSerializationAnnotation(
        "id",
        (1..size).map { "$it".repeat(20) },
        (1..size).associate { "$it".repeat(20) to "$it".repeat(20) },
    )

    companion object {
        private const val schema = "stream"
    }

    @Test()
    fun `save object using stream memory test`(): Unit = runBlocking {
        storeClient.store(annotatedLargeObject)
        val largeObjectFromDB = storeClient.findById<LargeObjectWithStreamSerializationAnnotation>("id")
        assertEquals(annotatedLargeObject, largeObjectFromDB)
    }

    @Test
    fun `save object as string memory test`() = runBlocking {
        storeClient.store(largeObject)
        val largeObjectFromDB = storeClient.findById<LargeObject>("id")
        assertEquals(largeObjectFromDB, largeObjectFromDB)
    }
}



