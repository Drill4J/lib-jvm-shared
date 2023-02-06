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
package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class Method(
    val ownerClass: String,
    val name: String,
    val desc: String,
    val hash: String?
) {

    val sign = "$name$desc"

    fun nameModified(otherMethod: Method) = hash == otherMethod.hash && desc == otherMethod.desc

    fun descriptorModified(otherMethod: Method) = name == otherMethod.name && hash == otherMethod.hash

    fun bodyModified(otherMethod: Method) = name == otherMethod.name && desc == otherMethod.desc

}

typealias Methods = List<Method>

@Serializable
data class MethodChanges(val map: Map<DiffType, List<Method>> = emptyMap()){
    val notEmpty: Boolean
        get() = map.keys
    .filter { it != DiffType.UNAFFECTED }
    .mapNotNull { map[it] }
    .flatten()
    .isNotEmpty()
}

data class BuildInfo(
    val buildVersion: String = "",
    val buildSummary: BuildSummary = BuildSummary(),
    val prevBuild: String = "",
    val methodChanges: MethodChanges = MethodChanges(),
    val classesBytes: Map<String, ByteArray> = emptyMap(),
    val javaMethods: Map<String, Methods> = emptyMap()
)

@Serializable
data class BuildSummary(
    val name: String = "",
    val addedDate: Long = 0,
    val totalMethods: Int = 0,
    val newMethods: Int = 0,
    val modifiedMethods: Int = 0,
    val unaffectedMethods: Int = 0,
    val deletedMethods: Int = 0
)

@Serializable
data class Base64Class(
    val className: String,
    val encodedBytes: String
)

enum class DiffType {
    MODIFIED_NAME,
    MODIFIED_DESC,
    MODIFIED_BODY,
    NEW,
    DELETED,
    UNAFFECTED
}