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
import com.epam.dsm.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.jetbrains.exposed.sql.transactions.*
import java.util.*
import kotlin.test.*

class BitSetTest : PostgresBased("bitset") {

    @Serializable
    data class BitsetClass(
        @Id
        val id: String,
        @Serializable(with = BitSetSerializer::class)
        val btst: BitSet
    )

    @Test
    fun shouldStoreBitSetInSeparateTable() = runBlocking {
        val size = 5_000
        val btst = BitSetInit(size)
            .apply {
                set(8)
                set(6)
                set(4)
                set(2)
                set(0)
            }
        val id = "someIDhere"
        storeClient.store(
            BitsetClass(
                id,
                btst
            )
        )
        println("finished")
        assertEquals(btst, storeClient.findById<BitsetClass>(id)?.btst)
    }

    @Test //todo split it to separate tests
    fun bitwiseOperations() = runBlocking {
        val size = 100
        println("starting...")
        (0..size).forEach { i ->
            val id = "someIDhere$i"
            println(id)
            storeClient.store(
                BitsetClass(
                    id,
                    BitSetInit(size)
                        .apply {
                            set(i)
                        }
                )
            )
        }

        val filledBitSet = BitSetInit(size).apply {
            (0..size).forEach {
                set(it)
            }
        }
        assertEquals(filledBitSet, bitwise("bit_or"))

        val emptyBitSet = BitSetInit(size)
        assertEquals(emptyBitSet, bitwise("bit_and"))
        println("finished")
    }
}


fun bitwise(name: String) = transaction {
    lateinit var final: BitSet
    execWrapper("select ${name}(Cast($JSON_COLUMN->>'btst' as BIT VARYING(10000000))) FROM bitset.bitset_class") {
        if (it.next())
            final = it.getString(1).toBitSet()
    }
    final
}

fun BitSetInit(nbits: Int): BitSet {
    return java.util.BitSet(nbits + 1)
        .apply { set(nbits) } //bitsetMagic
}
