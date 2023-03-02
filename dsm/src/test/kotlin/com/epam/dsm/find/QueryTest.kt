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
package com.epam.dsm.find

import com.epam.dsm.*
import com.epam.dsm.common.*
import com.epam.dsm.common.PrepareData.Companion.complexObject
import com.epam.dsm.common.PrepareData.Companion.payloadWithIdList
import com.epam.dsm.common.PrepareData.Companion.storeLists
import com.epam.dsm.common.PrepareData.Companion.testNameSecond
import kotlinx.coroutines.*
import kotlin.test.*

class QueryTest : PostgresBased("query") {
    @BeforeTest
    fun setUp() = runBlocking {
        storeClient.storeLists()
    }

    @Test
    fun `should return objects when findBy use query in default approach`() = runBlocking {
        val query: SearchQuery<PayloadWithIdList> = storeClient.findBy { PayloadWithIdList::id eq "1" }

        val result: List<PayloadWithIdList> = query.get()
        assertEquals(1, result.size)
        assertEquals(payloadWithIdList, result.first())
    }

    @Test
    fun `should find list of strings when execute with the field of sting`() = runBlocking {
        val query: SearchQuery<PayloadWithIdList> = storeClient.findBy { PayloadWithIdList::id eq "1" }

        assertEquals(listOf("1"), query.getAndMap(PayloadWithIdList::id))
    }

    @Test
    fun `should find string values when pass the params`() = runBlocking {
        val query: SearchQuery<PayloadWithIdList> = storeClient.findBy { PayloadWithIdList::id eq "1" }
        assertEquals(listOf("42"), query.getStrings(PayloadWithIdList::num.name))
    }

    @Test
    fun `should find string values when pass the inner object`() = runBlocking {
        storeClient.store(complexObject)
        val query: SearchQuery<ComplexObject> = storeClient.findBy { ComplexObject::id eq "str" }
        assertEquals(listOf("12"), query.getStrings(FieldPath(ComplexObject::blink.name, SubObject::int.name)))
    }

    @Test
    fun `should find ids when pass conditions`() = runBlocking {
        val query: SearchQuery<PayloadWithIdList> = storeClient.findBy { PayloadWithIdList::id eq "1" }
        assertEquals(listOf("49"), query.getIds())
    }


    @Test
    fun `should find in list table when pass the parent ids`() = runBlocking {
        val query = storeClient.findBy<SetPayload> {
            containsParentId(listOf("49"))
        }
        assertEquals(listOf(PrepareData.setPayloadWithTest, PrepareData.setPayloadTest2), query.get())
    }

    @Test
    fun `should find in list table when pass the ids and additional queries`() = runBlocking {
        val query = storeClient.findBy<SetPayload> {
            containsParentId(listOf("49")) and (SetPayload::id eq "first")
        }
        assertEquals(listOf(PrepareData.setPayloadWithTest), query.get())
    }


    @Test
    fun `should find list of list stings when execute with the list of objects`() = runBlocking {
        val findBy: SearchQuery<PayloadWithIdList> = storeClient.findBy { PayloadWithIdList::id eq "1" }
        assertEquals(2, findBy.getListIds(PayloadWithIdList::list.name).size)
    }

    @Test
    fun `should find object by value in list `() = runBlocking {
        val query = storeClient.findBy<PayloadWithIdList> {
            (PayloadWithIdList::num eq 42) and FieldPath(PayloadWithIdList::list).anyInCollection<SetPayload> {
                SetPayload::nameExample eq testNameSecond
            } and (PayloadWithIdList::num eq 42)
        }
        assertEquals(listOf(payloadWithIdList), query.get())
    }


    @Test
    fun `should find object by several conditions in list `() = runBlocking {
        sequenceOf(
            PayloadWithIdList("3", 0, "", listOf(SetPayload("Session", "One"), SetPayload("Session", "Two"))),
            PayloadWithIdList("4", 1, "", listOf(
                SetPayload("Session", "One"), SetPayload("Session", "Two"), SetPayload("Team", "Drill4j"),
            )),
            PayloadWithIdList("5", 2, "", listOf(SetPayload("Session", "One"), SetPayload("Team", "Drill4j"))),
            PayloadWithIdList("6", 3, "", listOf(
                SetPayload("Session", "One"),
                SetPayload("Team", "Report Portal"),
                SetPayload("Team", "Zapad"),
                SetPayload("Team", "Drill4j"))),
            PayloadWithIdList("7", 3, "", listOf(SetPayload("Session", "Two"), SetPayload("Team", "Report Portal")))
        ).forEach { storeClient.store(it) }

        val query1 = storeClient.findBy<PayloadWithIdList> {
            FieldPath(PayloadWithIdList::list).anyInCollection<SetPayload> {
                (SetPayload::id eq "Session") and (SetPayload::nameExample eq "One")
            }
        }
        assertEquals(4, query1.get().size)
        val query2 = storeClient.findBy<PayloadWithIdList> {
            FieldPath(PayloadWithIdList::list).anyInCollection<SetPayload> {
                (SetPayload::id eq "Session") and (SetPayload::nameExample contains listOf("One", "Two"))
            }
        }
        assertEquals(5, query2.get().size)


        val query3 = storeClient.findBy<PayloadWithIdList> {
            val initial = FieldPath(PayloadWithIdList::list).anyInCollection<SetPayload> {
                (SetPayload::id eq "Session") and (SetPayload::nameExample eq "One")
            }
            sequenceOf("Report Portal", "Drill4j").fold(initial) { acc2, value ->
                acc2 and (FieldPath(PayloadWithIdList::list).anyInCollection<SetPayload> {
                    (SetPayload::id eq "Team") and (SetPayload::nameExample eq value)
                })
            }
        }
        assertEquals(1, query3.get().size)
    }


}


