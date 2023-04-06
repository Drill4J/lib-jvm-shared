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
import com.epam.dsm.find.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.test.*
import kotlin.test.Test

class FewIdsTest : PostgresBased("id") {

    @Serializable
    data class TwoIds(
        @Id val first: String,
        @Id val second: String,
        val data: String,
    )

    private val sample = TwoIds("first", "second", "somthing")

    @Test
    fun `should store and retrieve with two ids`() = runBlocking {
        val second = sample.copy(second = "second_2222")
        storeClient.store(sample)
        storeClient.store(second)
        val all = storeClient.getAll<TwoIds>()
        assertEquals(2, all.size)
        assertEquals(listOf(second), storeClient.findBy<TwoIds> { TwoIds::second eq "second_2222" }.get())
    }


    @Ignore("not implement")
    @Test
    fun `should retrieve and delete by id`() = runBlocking {
        storeClient.store(sample)
        assertEquals(sample, storeClient.findById<TwoIds>("first"))
        storeClient.deleteById<TwoIds>("first")
        assertEquals(0, storeClient.getAll<TwoIds>().size)
    }


}

