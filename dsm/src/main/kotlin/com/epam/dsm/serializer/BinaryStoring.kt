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
import com.epam.dsm.util.*
import com.github.luben.zstd.*
import com.zaxxer.hikari.pool.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*


fun Transaction.storeBinary(id: String, value: ByteArray) {
    val prepareStatement = connection.prepareStatement(
        """
        |INSERT INTO BINARYA VALUES ('$id', ?)
        |ON CONFLICT (id) DO UPDATE SET BINARYA = excluded.BINARYA
    """.trimMargin(), false
    )
    prepareStatement[1] = Zstd.compress(value)
    prepareStatement.executeUpdate()
}

fun storeBinaryCollection(
    bytes: Iterable<ByteArray>,
): List<String> = transaction {
    val ids = mutableListOf<String>()
    runBlocking {
        createTableIfNotExists<Any>(connection.schema) { createBinaryTable() }
    }
    val statement = (connection.connection as HikariProxyConnection).prepareStatement(
        """
        |INSERT INTO BINARYA VALUES (?, ?)
        |ON CONFLICT (id) DO UPDATE SET BINARYA = excluded.BINARYA
    """.trimMargin()
    )
    bytes.forEachIndexed { index, value ->
        statement.setString(1, uuid.also { ids.add(it) })
        statement.setBytes(2, Zstd.compress(value))
        statement.addBatch()
        statement.clearParameters()
        if (index % DSM_PUSH_LIMIT == 0) {
            statement.executeBatch()
            statement.clearBatch()
        }
    }
    statement.executeBatch()
    ids
}

fun Transaction.getBinary(id: String): ByteArray {
    runBlocking {
        createTableIfNotExists<Any>(connection.schema) {
            createBinaryTable()
        }
    }
    val prepareStatement = connection.prepareStatement(
        "SELECT BINARYA FROM BINARYA WHERE $ID_COLUMN = ${id.toQuotes()}",
        false
    )
    val resultSet = prepareStatement.executeQuery()
    return if (resultSet.next()) {
        val bytes = resultSet.getBytes(1)
        Zstd.decompress(bytes, Zstd.decompressedSize(bytes).toInt())
    } else throw RuntimeException("Not found with id $id")
}

fun getBinaryCollection(ids: List<String>): Collection<ByteArray> = transaction {
    val entities = mutableListOf<ByteArray>()
    if (id.isEmpty()) return@transaction entities
    val schema = connection.schema
    runBlocking {
        createTableIfNotExists<Any>(schema) {
            createBinaryTable()
        }
    }
    val idString = ids.joinToString { "'$it'" }
    val stm = "SELECT BINARYA FROM BINARYA WHERE ID in ($idString)"
    val statement = (connection.connection as HikariProxyConnection).createStatement()
    statement.fetchSize = DSM_FETCH_LIMIT
    statement.executeQuery(stm).let { rs ->
        while (rs.next()) {
            val bytes = rs.getBytes(1)
            entities.add(Zstd.decompress(bytes, Zstd.decompressedSize(bytes).toInt()))
        }
    }
    return@transaction entities
}

fun Transaction.getBinaryAsStream(id: String): InputStream {
    val prepareStatement = connection.prepareStatement(
        "SELECT BINARYA FROM BINARYA " +
                "WHERE $ID_COLUMN = ${id.toQuotes()}", false
    )
    val resultSet = prepareStatement.executeQuery()
    return if (resultSet.next())
        ZstdInputStream(resultSet.getBinaryStream(1))
    else throw RuntimeException("Not found with id $id")

}
