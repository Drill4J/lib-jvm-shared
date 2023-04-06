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
import com.epam.dsm.common.PrepareData.Companion.setPayload
import com.epam.dsm.common.PrepareData.Companion.storeLists
import com.epam.dsm.common.PrepareData.Companion.setPayloadTest2
import com.epam.dsm.common.PrepareData.Companion.setPayloadWithTest
import com.epam.dsm.common.PrepareData.Companion.testName
import com.epam.dsm.common.PrepareData.Companion.testNameSecond
import com.epam.dsm.serializer.*
import com.epam.dsm.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.junit.jupiter.api.*
import kotlin.test.*
import kotlin.test.Test

class CustomQueryTest : PostgresBased("custom_query") {

    @Serializable
    data class ResultDto(
        val numParent: Int,
        val fieldSetPayload: String,
        val setPayload: SetPayload,
    )

    @Test
    fun `should create custom sql when few results`() = runBlocking {
        storeClient.storeLists()
        val result = storeClient.executeInTransaction {
            val tableName = SetPayload::class.tableName()
            val parentTableName = PayloadWithIdList::class.tableName()
            val containsIds = listOf("49").toSqlIn()
            val sql = """
                SELECT parent.${FieldPath(PayloadWithIdList::num).extractText()},
                       child.${FieldPath(SetPayload::nameExample).extractText()},
                       child.$JSON_COLUMN
                from $tableName as child,
                     $parentTableName as parent
                where child.$PARENT_ID_COLUMN $containsIds
                  and parent.$ID_COLUMN $containsIds;
            """.trimIndent()
            val result = mutableListOf<ResultDto>()
            logger.debug { "sql:\n$sql" }
            connection.prepareStatement(sql, false).executeQuery().let {
                while (it.next()) {
                    val f: Int = it.getInt(1)
                    val f2: String = it.getString(2)
                    val decodeFromString: SetPayload = json.decodeFromString(it.getString(3))
                    result.add(ResultDto(f, f2, decodeFromString))
                }
            }
            result
        }
        val expected = ResultDto(42, testName, setPayloadWithTest)
        val expected2 = ResultDto(42, testNameSecond, setPayloadTest2)
        assertEquals(listOf(expected, expected2), result)
    }

    @Test
    fun `should return map of objects when use group by`() = runBlocking {
        storeClient.storeLists()
        val groupBy = storeClient.executeInTransaction {
            val tableName = SetPayload::class.tableName()
            val payloadId = FieldPath(SetPayload::id).extractText()
            val sql = """
                SELECT $payloadId,
                       to_jsonb(json_agg($JSON_COLUMN))
                from $tableName
                group by $payloadId, $JSON_COLUMN #>> '{${SetPayload::nameExample.name}}'
            """.trimIndent()
            logger.debug { "sql: $sql" }
            val result = mutableMapOf<String, List<SetPayload>>()
            connection.prepareStatement(sql, false).executeQuery().let {
                while (it.next()) {
                    val key = it.getString(1)
                    val value: List<SetPayload> = dsmDecode(it.getString(2), classLoader<SetPayload>())
                    logger.debug { "result of dsm $value" }
                    result[key] = value
                }
            }
            result
        }
        val expected = mapOf(
            setPayloadWithTest.id to listOf(setPayloadWithTest),
            setPayloadTest2.id to listOf(setPayloadTest2),
            setPayload.id to listOf(setPayload)
        )
        assertEquals(expected, groupBy)
    }

}


