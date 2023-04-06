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
package com.epam.drill

import com.epam.dsm.*
import com.epam.dsm.test.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.test.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
class SaveTest : Configuration() {

    private val schema = "binary_perf_test"
    private val client = StoreClient(TestDatabaseContainer.createDataSource(schema = schema))
    private val size = 50_000
    private val largeObject = LargeObject(
        "id",
        (1..size).map { "$it".repeat(20) },
        (1..size).associate { "$it".repeat(20) to "$it".repeat(20) },
    )

    private val annotatedLargeObject = LargeObjectWithStreamAnnotation(
        "id",
        (1..size).map { "$it".repeat(20) },
        (1..size).associate { "$it".repeat(20) to "$it".repeat(20) },
    )

    @Setup
    fun before() {
        transaction {
            exec("CREATE SCHEMA IF NOT EXISTS $schema")
        }
        println("created schema")
    }

    @TearDown
    fun after() {
        println("after benchmark...")
        transaction {
            exec("DROP SCHEMA $schema CASCADE")
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(Threads.MAX)
    fun saveObjectAsJsonStream() = runBlocking {
        client.store(annotatedLargeObject)
        val largeObject = client.findById<LargeObjectWithStreamAnnotation>("id")
        assertEquals(annotatedLargeObject, largeObject)
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(Threads.MAX)
    fun saveObjectAsJsonString(): Unit = runBlocking {
        client.store(largeObject)
        val largeObjectFromDB = client.findById<LargeObject>("id")
        assertEquals(largeObject, largeObjectFromDB)
    }


}
