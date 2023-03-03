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

sealed class TsType {
    abstract val name: String
}

data class TsUnion(
    override val name: String,
    val types: List<String> = emptyList()
) : TsType()

data class TsInterface(
    override val name: String,
    val fields: List<TsField> = emptyList()
) : TsType()

data class TsField(
    val name: String,
    val type: String
)

fun Sequence<TsType>.appendTo(appendable: Appendable, module: String) {
    appendable.appendLine("declare module '$module' {")
    forEach { it.appendTo(appendable, indent = "  ", modifier = "export ") }
    appendable.appendLine("}")
}

fun TsType.render(): String = "${StringBuffer().also { appendTo(it) }}"

fun TsType.appendTo(
    appendable: Appendable,
    modifier: String = "",
    indent: String = ""
) {
    when(this) {
        is TsInterface -> {
            appendable.appendLine("$indent${modifier}interface $name {")
            fields.forEach {
                appendable.appendLine("$indent  ${it.name}: ${it.type};")
            }
            appendable.appendLine("$indent}")
        }
        is TsUnion -> {
            appendable.appendLine("$indent${modifier}type $name = ${types.joinToString(" | ")}")
        }
    }
}
