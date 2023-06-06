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
@file:Suppress("NOTHING_TO_INLINE")

package com.epam.dsm.util

import com.epam.dsm.*
import com.epam.dsm.Column
import com.epam.dsm.find.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import mu.*
import org.jetbrains.exposed.sql.*
import java.sql.*
import java.util.*
import kotlin.reflect.*

typealias PathToTable = Pair<String, String>
typealias EntryDescriptor = Pair<SerialDescriptor, SerialDescriptor>

internal val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

val logger = KotlinLogging.logger {}

internal val uuid
    get() = "${UUID.randomUUID()}"

inline fun <reified T : Any> Transaction.createJsonTable(
    tableName: String,
    descriptor: SerialDescriptor?
) {
    val columns = descriptor?.findColumnAnnotation() ?: emptyMap()
    execWrapper(
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            $ID_COLUMN varchar(256) not null constraint ${tableName}_pk primary key,
            $PARENT_ID_COLUMN varchar(256), 
            $JSON_COLUMN jsonb${columns.keys.tableColumns()}
        );
        ${cascadeDeleteCollectionTrigger<T>(tableName, "update", "NEW")}
        ${cascadeDeleteCollectionTrigger<T>(tableName, "delete", "OLD")}
        ${insertTrigger(tableName, columns)}
   """
    )
    commit()
}

fun Set<Column>.tableColumns() = takeIf { it.isNotEmpty() }?.joinToString(prefix = ", ") {
    "${it.name} ${it.type.value}"
} ?: ""

fun Transaction.createBinaryTable() {
    execWrapper(
        """
             CREATE TABLE IF NOT EXISTS BINARYA ($ID_COLUMN varchar(256) not null constraint binarya_pk primary key, binarya bytea, agentkey varchar(256));
             ALTER TABLE BINARYA ALTER COLUMN binarya SET STORAGE EXTERNAL; 
             """.trimIndent()
    )
    commit()
}

inline fun Transaction.createMapTable(tableName: String) {
    execWrapper("CREATE TABLE IF NOT EXISTS $tableName (ID varchar(256) not null constraint ${tableName}_pk primary key, $PARENT_ID_COLUMN varchar(256) not null, KEY_JSON jsonb, VALUE_JSON jsonb);")
    commit()
}

inline fun Transaction.execWrapper(
    sqlStatement: String,
    args: Iterable<Pair<IColumnType, Any?>> = emptyList(),
    noinline transform: (ResultSet) -> Unit = {},
) {
    logger.trace { "SQL statement on schema '${connection.schema}': $sqlStatement" }
    exec(sqlStatement, args, transform = transform)
}

// As per https://youtrack.jetbrains.com/issue/KT-10440 there isn't a reliable way to get back a KClass for a
// Kotlin primitive types
val PRIMITIVE_CLASSES = mapOf<SerialKind, KClass<*>>(
    PrimitiveKind.BOOLEAN to Boolean::class,
    PrimitiveKind.BYTE to Byte::class,
    PrimitiveKind.CHAR to Char::class,
    PrimitiveKind.FLOAT to Float::class,
    PrimitiveKind.DOUBLE to Double::class,
    PrimitiveKind.INT to Int::class,
    PrimitiveKind.LONG to Long::class,
    PrimitiveKind.SHORT to Short::class,
    PrimitiveKind.STRING to String::class,
)

fun SerialKind.isNotPrimitive() = PRIMITIVE_CLASSES[this] == null

private fun EntryDescriptor.tableName() = "${first.simpleName}_to_${second.simpleName}"

private val SerialDescriptor.simpleName
    get() = when (kind) {
        is StructureKind.LIST -> List::class.simpleName!!.lowercase(Locale.getDefault())
        is StructureKind.MAP -> Map::class.simpleName!!.lowercase(Locale.getDefault())
        else -> serialName.substringAfterLast(".").lowercase(Locale.getDefault())
    }


private val SerialDescriptor.elementsRange
    get() = 0..elementsCount.dec()

inline fun <reified T : Any> cascadeDeleteCollectionTrigger(
    tableName: String,
    operation: String,
    returnValue: String,
): String = run {
    val collectionPaths = T::class.serializerOrNull()?.descriptor?.collectionPaths() ?: emptyList()
    tableName.createTrigger(collectionPaths, operation, returnValue)
}

fun String.createTrigger(
    collectionPaths: List<PathToTable>,
    operation: String,
    returnValue: String,
): String {
    val triggerName = """trigger_${operation}_for_$this"""
    return "".takeIf { collectionPaths.isEmpty() } ?: """
        CREATE OR REPLACE FUNCTION $triggerName() 
        RETURNS TRIGGER LANGUAGE PLPGSQL AS ${'$'}$triggerName${'$'}
            BEGIN
                ${collectionPaths.toQuery()}
                RETURN $returnValue;
            END;
        ${'$'}$triggerName${'$'};
        
        DROP TRIGGER IF EXISTS $triggerName on $this;
        
        CREATE TRIGGER $triggerName
        BEFORE $operation ON $this
        FOR EACH ROW EXECUTE PROCEDURE $triggerName();
    """.trimIndent()
}

/**
 * Trigger on insert. The trigger fills a separate column (if there is a [Column] annotation)
 * from json_body, while the annotated [Column] fields are taken away from json_body itself.
 */
fun insertTrigger(tableName: String, columns: Map<Column, List<String>>): String = run {
    "".takeIf { columns.isEmpty() } ?: """  
       CREATE or REPLACE FUNCTION insert_trigger_for_$tableName()
       RETURNS TRIGGER
       AS $$
       BEGIN
            ${columns.fillColumn()}
            NEW.$JSON_COLUMN = NEW.$JSON_COLUMN${columns.minus()};
       RETURN NEW;
       END;
       $$
       LANGUAGE plpgsql;
        
       DROP TRIGGER IF EXISTS insert_trigger_for_$tableName on $tableName;
       
       CREATE TRIGGER insert_trigger_for_$tableName
       BEFORE INSERT ON $tableName
       FOR EACH ROW EXECUTE PROCEDURE insert_trigger_for_$tableName();
    """.trimIndent()
}

private fun Map<Column, List<String>>.minus() = values.joinToString { "#-'{${it.joinToString()}}'" }


fun SerialDescriptor.collectionPaths(path: String = ""): List<PathToTable> {
    val pathCollection = mutableListOf<PathToTable>()
    elementsRange.forEach { index ->
        val currentDesc = getElementDescriptor(index)
        when (currentDesc.kind) {
            is StructureKind.CLASS -> {
                pathCollection.addAll(currentDesc.collectionPaths("$path->'${getElementName(index)}'"))
            }

            is StructureKind.LIST -> {
                currentDesc.elementDescriptors.firstOrNull()?.takeIf { it.kind.isNotPrimitive() }?.let {
                    pathCollection.add("$path->>'${getElementName(index)}'" to it.tableName())
                }
            }

            is StructureKind.MAP -> {
                val (keyDest, valueDest) = currentDesc.getElementDescriptor(0) to currentDesc.getElementDescriptor(1)
                if (!keyDest.isPrimitiveKind() || !valueDest.isPrimitiveKind()) {
                    pathCollection.add("->>'${getElementName(index)}'" to (keyDest to valueDest).tableName())
                }
            }

            else -> {
                // do nothing
            }
        }
    }
    return pathCollection
}

fun List<PathToTable>.toQuery() = fold("") { acc, (path, table) ->
    acc + "DELETE FROM $table WHERE $ID_COLUMN IN (SELECT * FROM json_array_elements_text((OLD.$JSON_COLUMN$path)::json)); \n"
}

fun Map<Column, List<String>>.fillColumn() = entries.fold("") { acc, (annotation, path) ->
    acc + "NEW.${annotation.name} = CAST(NEW.$JSON_COLUMN${path.toSqlPath()} as ${annotation.type.value}); \n"
}

fun List<String>.toSqlPath(
    initial: String = "",
    lastOperation: String = EXTRACT_TEXT
) = foldIndexed(initial) { index, acc, next ->
    if (index != size - 1) "$acc $EXTRACT_JSON ${next.toQuotes()}" else "$acc $lastOperation ${next.toQuotes()}"
}

fun SerialDescriptor.buildJson() = findColumnAnnotation().entries.fold(JSON_COLUMN) { acc, (annotation, path) ->
    """jsonb_set($acc, '{${path.joinToString()}}'::text[], to_jsonb(${annotation.name}), true)""".trimIndent()
}

inline fun <reified T : Any> fullJson(
    descriptor: SerialDescriptor? = T::class.serializerOrNull()?.descriptor
) = descriptor?.buildJson() ?: JSON_COLUMN
