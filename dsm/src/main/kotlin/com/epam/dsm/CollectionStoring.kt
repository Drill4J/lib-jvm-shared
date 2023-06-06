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

import com.epam.dsm.serializer.*
import com.epam.dsm.util.*
import com.zaxxer.hikari.pool.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*
import java.util.*
import kotlin.reflect.*

/**
 * File is needed to don't keep a huge collection in memory
 */
fun <T : Any?> storeCollection(
    collection: Iterable<T>,
    parentId: String?,
    elementClass: KClass<*>,
    elementSerializer: KSerializer<Any>,
): List<String> = transaction {
    val ids = mutableListOf<String>()
    val tableName = runBlocking {
        createTableIfNotExists<Any>(connection.schema, elementClass.tableName(), elementSerializer.descriptor)
    }
    if (collection.none()) return@transaction ids
    val file = File.createTempFile("prefix-", "-postfix")
    try {
        val sizes = mutableListOf<Int>()
        file.outputStream().use { outputStream ->
            collection.filterNotNull().forEach { value ->
                val json = json.encodeToString(elementSerializer, value).intern()
                outputStream.write(json.toByteArray())
                sizes.add(json.length)
            }
        }
        val stmt = """
            |INSERT INTO ${tableName.lowercase(Locale.getDefault())} (ID, $PARENT_ID_COLUMN, $JSON_COLUMN) VALUES (?, ?, CAST(? as jsonb))
            |ON CONFLICT (id) DO UPDATE SET $JSON_COLUMN = excluded.$JSON_COLUMN
        """.trimMargin()
        val statement = (connection.connection as HikariProxyConnection).prepareStatement(stmt)
        logger.trace { "SERIALIZER_LOG___storeCollection___${file.length()}" }
        file.inputStream().reader().use {
            sizes.forEachIndexed { index, size ->
                statement.setString(1, uuid.also { ids.add(it) })
                statement.setString(2, parentId)
                statement.setCharacterStream(3, it, size)
                statement.addBatch()
                if (index % DSM_PUSH_LIMIT == 0) {
                    statement.executeBatch()
                    statement.clearBatch()
                }
            }
            statement.executeBatch()
        }
    } finally {
        file.delete()
    }
    ids
}

/**
 * Loading of collection by regular expression: by prefix which is parentId plus parentIndex
 */
inline fun <reified T : Any> loadCollection(
    ids: List<String>,
    elementClass: KClass<*>,
    elementSerializer: KSerializer<T>,
): Iterable<T> = transaction {
    val entities: MutableList<T> = mutableListOf()
    if (ids.isEmpty()) return@transaction entities
    val tableName = runBlocking {
        createTableIfNotExists<Any>(connection.schema, elementClass.tableName())
    }
    val idString = ids.joinToString { "'$it'" }
    val stm = "select ${fullJson<Any>(elementSerializer.descriptor)} FROM $tableName WHERE $ID_COLUMN in ($idString)"
    val statement = (connection.connection as HikariProxyConnection).prepareStatement(stm)
    statement.fetchSize = DSM_FETCH_LIMIT
    statement.executeQuery().let { rs ->
        while (rs.next()) {
            val element = json.decodeFromStream(elementSerializer, rs.getBinaryStream(1))
            entities.add(element)
        }
    }
    return@transaction entities
}
