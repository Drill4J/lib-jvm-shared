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
package com.epam.drill.ts.kt2dts

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

fun Sequence<Descriptor>.convert(): Sequence<TsType> = mapNotNull { descriptor ->
    val (klass, serDe, descendants) = descriptor
    when (serDe.kind) {
        is PolymorphicKind -> descendants.takeIf { it.any() }?.let {
            TsUnion(klass.simpleName!!, descendants.mapNotNull(AnyKlass::simpleName))
        }
        StructureKind.CLASS -> {
            val klassName = klass.simpleName ?: serDe.serialName
            val discriminator = klass.annotations.filterIsInstance<SerialName>().firstOrNull()?.let {
                listOf(TsField("type", "'${it.value}'"))
            } ?: emptyList()
            val classProps = klass.memberProperties.associate { it.name to it.returnType }
            val fields = serDe.elementDescriptors.mapIndexed { i, ed ->
                val name = serDe.getElementName(i)
                val kType: KType = classProps.getValue(name)
                val opt = "?".takeIf { serDe.isElementOptional(i) } ?: ""
                TsField(
                    name = "$name$opt",
                    type = ed.toTsType(kType)
                )
            }
            TsInterface(klassName, discriminator + fields)
        }
        else -> null
    }
}

private fun SerialDescriptor.toTsType(kType: KType): String = when (kind) {
    PrimitiveKind.STRING -> "string"
    PrimitiveKind.BOOLEAN -> "boolean"
    is PrimitiveKind -> numberType()
    SerialKind.ENUM -> elementNames.joinToString(" | ") { "'$it'" }
    StructureKind.LIST -> kType.toTsArrayType()
    StructureKind.MAP -> kType.toTsIndexType()
    StructureKind.CLASS, is PolymorphicKind -> "$kType".substringAfterLast('.')
    else -> null
}?.let { if (isNullable) "${it.trimEnd('?')} | null" else it } ?: error("Unsupported type: $kType")

private fun SerialDescriptor.numberType(): String? = when (kind) {
    PrimitiveKind.BYTE,
    PrimitiveKind.SHORT,
    PrimitiveKind.INT,
    PrimitiveKind.LONG,
    PrimitiveKind.FLOAT,
    PrimitiveKind.DOUBLE -> "number"
    else -> null
}

private fun KType.toTsIndexType(): String? = run {
    val (keyType, valType) = arguments
    keyType.type?.toTsType()?.takeIf { it == "string" || it == "number" }?.let { tsKeyType ->
        valType.type?.run {
            toTsArrayType() ?: toTsType()
        }?.let { "{ [key: $tsKeyType]: $it }" }
    }?.let(::optNull)
}

private fun KType.toTsArrayType(): String? = takeIf { arguments.size == 1 }?.run {
    val (arg) = arguments
    arg.type?.toTsType()?.let {
        "${if ('|' in it) "($it)" else it}[]"
    }?.let(::optNull)
}

private fun KType.toTsType() = toTsArrayType() ?: (classifier as? KClass<*>)?.run {
    when (this) {
        String::class, Boolean::class -> simpleName?.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class -> "number"
        else -> simpleName
    }?.let(::optNull)
}

private fun KType.optNull(tsType: String) = if (isMarkedNullable) "$tsType | null" else tsType
