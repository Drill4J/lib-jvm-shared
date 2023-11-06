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
@file:Suppress("CovariantEquals", "BlockingMethodInNonBlockingContext")

package com.epam.dsm

import com.epam.dsm.find.*
import com.epam.dsm.serializer.*
import com.epam.dsm.util.*
import com.zaxxer.hikari.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.jdbc.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.*
import kotlin.time.*

const val JSON_COLUMN = "JSON_BODY"
const val ID_COLUMN = "ID"
const val PARENT_ID_COLUMN = "PARENT_ID"

/**
 * Can be init by:
 * @see HikariConfig - this useful for production
 * @see HikariDataSource - it can be useful for tests, you can close pool connection
 */
class StoreClient(val hikariConfig: HikariConfig) : AutoCloseable {
    private val hikariDataSource =
        if (hikariConfig is HikariDataSource) hikariConfig else HikariDataSource(hikariConfig)
    private val database = DatabaseFactory.init(hikariDataSource)
    private val schema = hikariDataSource.schema

    init {
//      todo transform schema name?
        transaction {
            execWrapper("CREATE SCHEMA IF NOT EXISTS $schema")
        }
    }

    override fun close() {
        logger.debug { "close store client with $schema..." }
        TransactionManager.closeAndUnregister(database)
        hikariDataSource.close()
    }

    suspend fun <T> executeInAsyncTransaction(block: suspend Transaction.() -> T) = withContext(Dispatchers.IO) {
        newSuspendedTransaction(db = database) {
            block(this)
        }
    }

    fun <T> executeInTransaction(block: Transaction.() -> T): T = transaction(db = database) {
        block(this)
    }

    suspend inline fun <reified T : Any> store(any: T) = run {
        executeInAsyncTransaction {
            store(any)
        }
        any
    }

    suspend inline fun <reified T : Any> getAll(): Collection<T> =
        executeInAsyncTransaction {
            getAll()
        }

    suspend inline fun <reified T : Any> findById(id: Any): T? =
        withContext(Dispatchers.IO) {
            executeInAsyncTransaction {
                findById(id)
            }
        }

    suspend inline fun <reified T : Any> findBy(noinline expression: Expr<T>.() -> Unit) =
        withContext(Dispatchers.IO) {
            executeInAsyncTransaction {
                findBy(expression)
            }
        }

    suspend inline fun <reified T : Any> deleteById(id: Any): Unit =
        withContext(Dispatchers.IO) {
            executeInAsyncTransaction {
                deleteById<T>(id)
            }
        }


    suspend inline fun <reified T : Any> deleteBy(noinline expression: Expr<T>.() -> Unit) =
        withContext(Dispatchers.IO) {
            executeInAsyncTransaction {
                deleteBy(expression)
            }
        }

    suspend inline fun <reified T : Any> deleteAll(): Unit =
        withContext(Dispatchers.IO) {
            executeInAsyncTransaction {
                deleteAll<T>()
            }

        }
}

suspend inline fun <reified T : Any> Transaction.getAll(): MutableList<T> {
    val finalData = mutableListOf<T>()
    val tableName = createTableIfNotExists<T>(connection.schema)
    val classLoader = T::class.java.classLoader
    execWrapper("select ${fullJson<T>()} FROM $tableName") { rs ->
        while (rs.next()) {
            finalData.add(dsmDecode(rs.getBinaryStream(1), classLoader))
        }
    }
    return finalData
}

inline fun <reified T : Any> Transaction.findBy(
    expression: Expr<T>.() -> Unit,
): SearchQuery<T> = SearchQuery(buildSqlCondition(expression), db, classLoader<T>())

suspend inline fun <reified T : Any> Transaction.findById(
    id: Any,
): T? = run {
    var entity: T? = null
    val tableName = createTableIfNotExists<T>(connection.schema)
    execWrapper("select ${fullJson<T>()} FROM $tableName WHERE $ID_COLUMN='${id.hashCode()}'") { rs ->
        if (rs.next()) {
            entity = dsmDecode(rs.getBinaryStream(1))
        }
    }
    entity
}

suspend inline fun <reified T : Any> Transaction.deleteById(
    id: Any,
) {
    val tableName = createTableIfNotExists<T>(connection.schema)
    execWrapper("DELETE FROM $tableName WHERE $ID_COLUMN='${id.hashCode()}'") //todo use parameters to detect type
}

suspend inline fun <reified T : Any> Transaction.deleteBy(
    noinline expression: Expr<T>.() -> Unit,
) {
    val tableName = createTableIfNotExists<T>(connection.schema)
    execWrapper(
        """
                    |DELETE FROM $tableName
                    |WHERE ${buildSqlCondition(expression)}
                    """.trimMargin()
    )
}

suspend inline fun <reified T : Any> Transaction.deleteAll() {
    val tableName = createTableIfNotExists<T>(connection.schema)
    execWrapper("DELETE FROM $tableName")
}

suspend inline fun <reified T : Any> Transaction.store(any: T) {
    run {
        val schema = connection.schema
        val tableName = createTableIfNotExists<T>(schema)
        val measureTime = measureTime {
            if (T::class.annotations.any { it is StreamSerialization }) {
                storeAsStream(any, tableName)
            } else {
                storeAsString(any, tableName)
            }
        }
        logger.trace { "Store object took: $measureTime" }
    }
}

inline fun <reified T : Any> Transaction.storeAsString(
    any: T,
    tableName: String,
) {
    val id = any.id()
    val stmt =
        """
            |INSERT INTO ${tableName.lowercase(Locale.getDefault())} ($ID_COLUMN, $JSON_COLUMN) VALUES ('$id', CAST(? as jsonb))
            |ON CONFLICT ($ID_COLUMN) DO UPDATE SET $JSON_COLUMN = excluded.$JSON_COLUMN
        """.trimMargin()
    val stm = connection.prepareStatement(stmt, false)
    stm[1] = json.encodeToString(T::class.dsmSerializer(id), any)
    logger.trace { "query: $stmt\nvalue: $any" }
    stm.executeUpdate()
}

/**
 * Serialization of large objects takes a lot of memory. So we needed to use stream serialization.
 * But Json needed an OutputStream and an InputStream for Statement.
 * So we created the layer from the file
 */
inline fun <reified T : Any> Transaction.storeAsStream(
    any: T,
    tableName: String,
) {
    val id = any.id()
    val file = File.createTempFile("prefix-", "-suffix") // TODO EPMDJ-9370 Remove file creating
    try {
        file.outputStream().use {
            json.encodeToStream(T::class.dsmSerializer(id), any, it)
        }
        val stmt =
            """
            |INSERT INTO ${tableName.lowercase(Locale.getDefault())} (ID, $JSON_COLUMN) VALUES ('$id', CAST(? as jsonb))
            |ON CONFLICT (id) DO UPDATE SET $JSON_COLUMN = excluded.$JSON_COLUMN
        """.trimMargin()
        InputStreamReader(file.inputStream()).use {
            val prepareStatement = connection.prepareStatement(stmt, false) as JdbcPreparedStatementImpl
            prepareStatement.statement.setCharacterStream(1, it)
            prepareStatement.executeUpdate()
        }
    } finally {
        file.delete()
    }
}

val createdTables = ConcurrentHashMap.newKeySet<String>()
val mutex = Mutex()

fun KClass<*>.tableName() = camelRegex.replace(simpleName!!) {
    "_${it.value}"
}.lowercase(Locale.getDefault())

fun SerialDescriptor.tableName(): String {
    if (isCollectionElementType(ByteArray::class)) return "BINARYA"
    return camelRegex.replace(serialName.substringAfterLast(".")) {
        "_${it.value}"
    }.lowercase(Locale.getDefault())
}

fun EntryClass<*, *>.tableName(
) = "${first.simpleName!!.lowercase(Locale.getDefault())}_to_${second.simpleName!!.lowercase(Locale.getDefault())}"

/**
 * Retrieving a table name from a given class, when the table doesn't exist, creates it.
 * By default, creates json table.
 */
suspend inline fun <reified T : Any> createTableIfNotExists(
    schema: String,
    tableName: String = T::class.tableName(),
    descriptor: SerialDescriptor? = T::class.serializerOrNull()?.descriptor,
    crossinline createTable: Transaction.(String) -> Unit = { table ->
        createJsonTable<T>(table, descriptor)
    },
): String {
    val tableKey = "$schema.$tableName"
    if (!createdTables.contains(tableKey)) {
        mutex.withLock {
            logger.trace { "check after lock $tableKey in $createdTables " }
            if (!createdTables.contains(tableKey)) {
                transaction {
                    createTable(tableName)
                }
                createdTables.add(tableKey)
            }
        }
    }
    return tableName
}


