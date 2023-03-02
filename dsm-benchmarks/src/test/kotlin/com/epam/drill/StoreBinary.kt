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
import com.epam.dsm.serializer.*
import com.epam.dsm.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.random.*
import kotlin.test.*

@State(Scope.Benchmark)
@Fork
@Warmup(iterations = 3)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
class StoreBinary : Configuration() {

    private val schema = "binary_perf_test"
    private val client = StoreClient(TestDatabaseContainer.createDataSource(schema = schema))

    private val byteArrayWrapper = ByteArrayWrapper("id", Random.nextBytes(10000 * 200))

    private val arrayListWrapper = ArrayListWrapper("id1", (1..200).map { Random.nextBytes(10000) })


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(Threads.MAX)
    fun storeBinary() = runBlocking {
        client.store(byteArrayWrapper)
        val stored = client.findById<ByteArrayWrapper>("id")
        assertNotNull(stored)
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(Threads.MAX)
    fun storeBinaryList() = runBlocking {
        client.store(arrayListWrapper)
        val stored = client.findById<ArrayListWrapper>("id1")!!
        assertNotNull(stored)
    }

}

@Serializable
data class ArrayListWrapper(
    @Id
    val id: String,
    val classBytes: List<ByteArray>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayListWrapper

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Serializable
data class ByteArrayWrapper(
    @Id
    val id: String,
    @Serializable(with = BinarySerializer::class)
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayWrapper

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
