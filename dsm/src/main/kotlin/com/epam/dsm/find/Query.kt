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
import com.epam.dsm.Column
import com.epam.dsm.serializer.*
import com.epam.dsm.util.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import java.sql.*
import kotlin.reflect.*
import kotlin.reflect.full.*

class SearchQuery<T>(
    val expression: String,
    val db: Database,
    val classLoader: ClassLoader,
) {
    var transformReturn: (String) -> String = { it }

    //in future can be impl wrapper for other cases
    fun distinct(): SearchQuery<T> {
        transformReturn = {
            "DISTINCT $it"
        }
        return this
    }
}

inline fun <reified T> deserializeByDsm(): (ResultSet, ClassLoader) -> T = { rs, classLoader ->
    dsmDecode(rs.getBinaryStream(1), classLoader)
}

/**
 * It is for inner need. Use another methods
 */
suspend inline fun <reified T : Any, reified R : Any> SearchQuery<T>.execute(
    selectSql: String = fullJson<T>(),
    crossinline handleResult: (ResultSet, ClassLoader) -> R,
): List<R> = newSuspendedTransaction(db = db) {
    val tableName = createTableIfNotExists<T>(connection.schema)
    val resultList = mutableListOf<R>()
    execWrapper(
        """
            |SELECT ${transformReturn(selectSql)} FROM $tableName
            |WHERE $expression
        """.trimMargin()
    ) { rs ->
        while (rs.next()) {
            resultList.add(handleResult(rs, classLoader))
        }
    }
    resultList
}


/**
 * @return List<T> - full object that stores in DB
 */
suspend inline fun <reified T : Any> SearchQuery<T>.get(): List<T> = run { execute(handleResult = deserializeByDsm()) }

/**
 * @return List<String> ids of objects
 * @see ID_COLUMN
 */
suspend inline fun <reified T : Any> SearchQuery<T>.getIds(): List<String> =
    run { execute("to_jsonb($ID_COLUMN)", deserializeByDsm()) }

/**
 * @return List<R>, R - field which map
 */
suspend inline fun <reified T : Any, reified R : Any> SearchQuery<T>.getAndMap(field: KProperty1<T, R>): List<R> = run {
    val path = field.takeIf { it.hasAnnotation<Column>() }?.let { ColumnPath(it) } ?: FieldPath(field)
    execute(path.extractJson(), deserializeByDsm())
}

/**
 * @return List<String> - cast any type to String
 */
suspend inline fun <reified T : Any> SearchQuery<T>.getStrings(field: String): List<String> = run {
    val path = T::class.memberProperties.find { it.name == field }?.takeIf { it.hasAnnotation<Column>() }?.let {
        ColumnPath(it)
    } ?: FieldPath(field)
    getStrings(path)
}

suspend inline fun <reified T : Any> SearchQuery<T>.getStrings(field: Path): List<String> = run {
    execute(field.extractText()) { rs, _ -> rs.getString(1) as String }
}

/**
 * @return List<String> list ids of objects
 */
suspend inline fun <reified T : Any> SearchQuery<T>.getListIds(listName: String): List<String> = run {
    execute<T, List<String>>(FieldPath(listName).extractJson()) { rs, _ ->
        json.decodeFromStream(rs.getBinaryStream(1))
    }.flatten()
}


