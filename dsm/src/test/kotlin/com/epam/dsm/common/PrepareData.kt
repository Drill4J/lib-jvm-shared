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
package com.epam.dsm.common

import com.epam.dsm.*

class PrepareData {

    companion object {
        val blink = SubObject("subStr", 12, Last(2.toByte()))
        val testName = "test1"
        val setPayloadWithTest = SetPayload(
            id = "first", nameExample = testName, blink,
        )
        val setPayload = SetPayload(id = "third", nameExample = testName, blink)
        val testNameSecond = "test2"
        val setPayloadTest2 = SetPayload(id = "second", nameExample = testNameSecond, blink)
        val payloadWithIdList = PayloadWithIdList(
            id = "1",
            num = 42,
            list = listOf(
                setPayloadWithTest,
                setPayloadTest2
            )
        )
        val payloadWithIdList2 = PayloadWithIdList(
            id = "2",
            num = 32,
            list = listOf(setPayload)
        )

        /**
         * Example of what structure stores:
         * PayloadWithIdList:
        ID   | PARENT_ID | JSONB
        49   |   null    |"{"id": "1", "num": 42, "list": "['uuid','uuid']"}"
        50   |   null    |"{"id": "2", "num": 32, "list": "['uuid']"}"
        SetPayload
        ID   | PARENT_ID | JSONB
        uuid |   49      |"{"id": "first", "subObject": {"int": 12, "last": {"string": 2}, "string": "subStr"}, "nameExample": "test1"}"
        uuid |   49      |"{"id": "second", "subObject": {"int": 12, "last": {"string": 2}, "string": "subStr"}, "nameExample": "test2"}"
        uuid |   50      |"{"id": "third", "subObject": {"int": 12, "last": {"string": 2}, "string": "subStr"}, "nameExample": "test1"}"
         */
        suspend fun StoreClient.storeLists() {
            store(payloadWithIdList)
            store(payloadWithIdList2)
        }

        val last = Last(2.toByte())
        val complexObject = ComplexObject("str", 'x', blink, EnumExample.SECOND, null)
        val simpleObject = SimpleObject("id", "subStr", 12, last)
    }
}

