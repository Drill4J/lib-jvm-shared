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
import java.io.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork
@Warmup(iterations = 3)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
class CompressTest : Configuration() {

    private val filePath = ""
    private val bytes = File(filePath).readBytes()
    private val schema = "binary_perf_test"
    private val store = StoreClient(TestDatabaseContainer.createDataSource(schema = schema))

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
            val result = connection.prepareStatement(
                "SELECT pg_size_pretty(pg_total_relation_size('$schema.BINARYA'))", false
            ).executeQuery()
            result.next()
            println("Table size ${result.getString(1)}")
        }
        transaction {
            exec("DROP SCHEMA $schema CASCADE")
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(1)
    fun test() = runBlocking {
        store.store(BinaryClass("${java.util.UUID.randomUUID()}", bytes))
    }
}

