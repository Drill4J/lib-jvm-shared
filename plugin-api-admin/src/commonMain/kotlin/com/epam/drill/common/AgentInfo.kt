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

//TODO remove
data class AgentInfo(
    val id: String,
    val agentType: String,
    val serviceGroup: String = "",
    val buildVersion: String,
    val agentVersion: String = "",
    val name: String = "",
    val environment: String = "",
    val description: String = "",
    val sessionIdHeaderName: String = "",
    val adminUrl: String = "",
    val ipAddress: String = ""
) {
    override fun equals(
        other: Any?
    ): Boolean = other is AgentInfo && id == other.id && buildVersion == other.buildVersion

    override fun hashCode(): Int = 31 * id.hashCode() + buildVersion.hashCode()
}
