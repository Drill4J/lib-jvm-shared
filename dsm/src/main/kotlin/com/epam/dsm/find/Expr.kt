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
import com.epam.dsm.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

const val EXTRACT_TEXT = "->>"
const val EXTRACT_JSON = "->"

/**
 * FieldPath is using to search for a path starting with JSON_BODY
 */
class FieldPath : Path {
    constructor(vararg properties: KProperty1<*, *>) : super(properties.map { it.name })

    constructor(vararg fields: String) : super(fields.toList())

    constructor(fields: List<String>) : super(fields)

    /**
     * Build sql path starting with JSON_BODY.
     * @param lastOperation - operator -> or ->> before last path element
     * Return example: json_body -> 'path' -> 'to' ->> 'field'
     */
    override fun buildSqlPath(lastOperation: String): String {
        return path.toSqlPath(JSON_COLUMN, lastOperation)
    }
}

/**
 * ColumnPath is using to search for a path starting the separate column name
 */
class ColumnPath(
    vararg properties: KProperty1<*, *>,
) : Path(properties.map { it.findAnnotation<Column>()?.name ?: it.name }) {

    /**
     * Build sql path starting with name of separate column. First path element should be annotated with [Column]
     * @param lastOperation - operator -> or ->> before last path element
     * Return example: column -> 'path' -> 'to' ->> 'field'
     */
    override fun buildSqlPath(lastOperation: String): String {
        return path.drop(1).toSqlPath(path.first(), lastOperation)
    }
}

abstract class Path(fields: List<String>) {
    val path: List<String> = fields

    fun extractText(): String {
        return buildSqlPath(EXTRACT_TEXT)
    }

    fun extractJson(): String {
        return buildSqlPath(EXTRACT_JSON)
    }

    abstract fun buildSqlPath(lastOperation: String): String
}

/**
 * This class for building 'where' query
 * @see buildSqlCondition
 */
class Expr<Q : Any> {
    val conditions = mutableListOf<String>()

    companion object {
        const val SHIFT_OPERATION = 1
        const val SQL_LOWER_CASE = "lower"
        const val ANY = "%"
    }

    infix fun Expr<Q>.and(@Suppress("UNUSED_PARAMETER") expression: Expr<Q>): Expr<Q> {
        conditions.add(conditions.size - SHIFT_OPERATION, "AND")
        return this
    }

    infix fun Expr<Q>.or(@Suppress("UNUSED_PARAMETER") expression: Expr<Q>): Expr<Q> {
        conditions.add(conditions.size - SHIFT_OPERATION, "OR")
        return this
    }

    infix fun <R : Any?> KProperty1<Q, R>.eq(another: R): Expr<Q> {
        val encodeId = another.encodeId()
        conditions.add(
            """
            ${columnOrJsonBody(if (encodeId.startsWith("{")) "$JSON_COLUMN->" else "$JSON_COLUMN->>")}
            ${equal(encodeId)}
            """.trimIndent()
        )
        return this@Expr
    }

    infix fun <R : Any?> KProperty1<Q, R>.eqIgnoreCase(another: R): Expr<Q> {
        val encodeId = another.encodeId()
        conditions.add(
            """
            $SQL_LOWER_CASE(${columnOrJsonBody(if (encodeId.startsWith("{")) "$JSON_COLUMN->" else "$JSON_COLUMN->>")})
            ${equal(encodeId, true)}
            """.trimIndent()
        )
        return this@Expr
    }

    infix fun Path.eq(value: String): Expr<Q> {
        conditions.add("${extractText()} ${equal(value)}")
        return this@Expr
    }

    infix fun Path.eqIgnoreCase(value: String): Expr<Q> {
        conditions.add("$SQL_LOWER_CASE(${extractText()})${equal(value, ignoreCase = true)}")
        return this@Expr
    }

    private fun equal(
        value: String,
        ignoreCase: Boolean = false,
    ): String = if (!ignoreCase) "= ${value.toQuotes()}" else "= $SQL_LOWER_CASE(${value.toQuotes()})"

    fun Path.eqNull(): Expr<Q> {
        conditions.add("${extractText()}${this@Expr.eqNull()}")
        return this@Expr
    }

    fun <R : Any?> KProperty1<Q, R>.eqNull(): Expr<Q> {
        conditions.add("${columnOrJsonBody()}${this@Expr.eqNull()}")
        return this@Expr
    }

    private fun eqNull() = " is null"

    infix fun <R : Any?> KProperty1<Q, R>.startsWith(prefix: String): Expr<Q> {
        conditions.add("${columnOrJsonBody()} like '$prefix$ANY'")
        return this@Expr
    }

    infix fun <R : Any?> KProperty1<Q, R>.like(expr: String): Expr<Q> {
        conditions.add("${columnOrJsonBody()} like '$expr'")
        return this@Expr
    }

    infix fun <R : Any?> KProperty1<Q, R>.contains(values: List<String>): Expr<Q> {
        conditions.add("${columnOrJsonBody()}${values.toSqlIn()}")
        return this@Expr
    }

    private fun <R : Any?> KProperty1<Q, R>.columnOrJsonBody(
        startPath: String = "$JSON_COLUMN->>",
    ) = findAnnotation<Column>()?.name ?: "$startPath${this.name.toQuotes()}"

    infix fun Path.containsWithNull(values: List<String>): Expr<Q> {
        val pathField = extractText()
        conditions.add("($pathField ${values.toSqlIn()} OR $pathField${this@Expr.eqNull()})")
        return this@Expr
    }

    infix fun Path.contains(values: List<String>): Expr<Q> {
        conditions.add("${extractText()} ${values.toSqlIn()}")
        return this@Expr
    }

    infix fun Expr<Q>.containsId(list: List<String>): Expr<Q> {
        conditions.add("$ID_COLUMN ${list.toSqlIn()}")
        return this
    }

    infix fun Expr<Q>.containsParentId(list: List<String>): Expr<Q> {
        conditions.add("$PARENT_ID_COLUMN ${list.toSqlIn()}")
        return this
    }

    inline fun <reified T : Any> FieldPath.anyInCollection(
        expression: Expr<T>.() -> Unit,
    ): Expr<Q> {
        conditions.add(
            "(${extractText()})::jsonb ??| (SELECT ARRAY((Select $ID_COLUMN::text FROM ${T::class.tableName()} WHERE ${
                buildSqlCondition(
                    expression
                )
            })))"
        )
        return this@Expr
    }

    inline fun <reified T : Any> FieldPath.allInCollection(
        expression: Expr<T>.() -> Unit,
    ): Expr<Q> {
        conditions.add(
            "(${extractText()})::jsonb ??& (SELECT ARRAY((Select $ID_COLUMN::text FROM ${T::class.tableName()} WHERE ${
                buildSqlCondition(
                    expression
                )
            })))"
        )
        return this@Expr
    }

}

inline fun <reified T : Any> buildSqlCondition(
    expression: Expr<T>.() -> Unit,
) = Expr<T>().run { expression(this); conditions.joinToString(" ") }

fun List<String>.toSqlIn(): String = this.joinToString(prefix = "in ('", postfix = "')", separator = "', '")
