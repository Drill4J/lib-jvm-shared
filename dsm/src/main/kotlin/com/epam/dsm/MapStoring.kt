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
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*
import java.util.*
import kotlin.reflect.*

typealias EntrySize = Pair<Int, Int>
typealias EntryClass<T, R> = Pair<KClass<T>, KClass<R>>
typealias EntrySerializer<T, R> = Pair<KSerializer<T>, KSerializer<R>>

/**
 * File is needed to don't keep a huge map in memory
 */
//todo EPMDJ-10155 need to investigate other ways to store map, then add support for separate Column
fun <T : Any?, R : Any?> storeMap(
    map: Map<T, R>,
    parentId: String?,
    entryClass: EntryClass<*, *>,
    serializer: EntrySerializer<Any, Any>,
): List<String> = transaction {
    val ids = mutableListOf<String>()
    val tableName = runBlocking {
        createTableIfNotExists<Any>(connection.schema, entryClass.tableName()) {
            createMapTable(it)
        }
    }
    if (map.none()) return@transaction ids
    val file = File.createTempFile("prefix-", "-postfix") // TODO EPMDJ-9370 Remove file creating
    val sizes = mutableListOf<EntrySize>()
    try {
        file.outputStream().use {
            map.forEach { (key, value) ->
                if (key != null && value != null) {
                    val keyJson = json.encodeToString(serializer.first, key).intern()
                    val valueJson = json.encodeToString(serializer.second, value).intern()
                    sizes.add(keyJson.length to valueJson.length)
                    it.write(keyJson.toByteArray())
                    it.write(valueJson.toByteArray())
                }
            }
        }
        val stmt = """
            |INSERT INTO ${tableName.lowercase(Locale.getDefault())} ($ID_COLUMN, $PARENT_ID_COLUMN, KEY_JSON, VALUE_JSON) VALUES (?, ?, CAST(? as jsonb), CAST(? as jsonb))
            |ON CONFLICT (id) DO UPDATE SET KEY_JSON = excluded.KEY_JSON, VALUE_JSON = excluded.VALUE_JSON
        """.trimMargin()
        val statement = (connection.connection as HikariProxyConnection).prepareStatement(stmt)
        file.inputStream().reader().use {
            sizes.forEachIndexed { index, (keySize, valueSize) ->
                statement.setString(1, uuid.also { ids.add(it) })
                statement.setString(2, parentId)
                statement.setCharacterStream(3, it, keySize)
                statement.setCharacterStream(4, it, valueSize)
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
 * Loading of map by regular expression: by prefix which is parentId plus parentIndex
 */
inline fun <reified T : Any, reified R : Any> loadMap(
    ids: List<String>,
    clazz: EntryClass<T, R>,
    serializer: EntrySerializer<T, R>,
): Map<T, R> = transaction {
    val entities: MutableMap<T, R> = mutableMapOf()
    val tableName = clazz.tableName()
    runBlocking {
        createTableIfNotExists<Any>(connection.schema, tableName) {
            createMapTable(tableName)
        }
    }
    if (ids.isEmpty()) return@transaction entities
    val idString = ids.joinToString { "'$it'" }
    val stm = "select KEY_JSON, VALUE_JSON FROM $tableName WHERE $ID_COLUMN in ($idString)"
    val statement = (connection.connection as HikariProxyConnection).prepareStatement(stm)
    statement.fetchSize = DSM_FETCH_LIMIT
    statement.executeQuery().let { rs ->
        while (rs.next()) {
            val key = json.decodeFromStream(serializer.first, rs.getBinaryStream(1))
            val value = json.decodeFromStream(serializer.second, rs.getBinaryStream(2))
            entities[key] = value
        }
    }
    return@transaction entities
}

@Suppress("UNCHECKED_CAST")
fun ClassLoader.getClass(descriptor: SerialDescriptor): KClass<Any> = when (val kind = descriptor.kind) {
    is PrimitiveKind -> PRIMITIVE_CLASSES[kind]
    is StructureKind.LIST -> List::class
    is StructureKind.MAP -> Map::class
    else -> loadClass(descriptor.serialName).kotlin
} as KClass<Any>
