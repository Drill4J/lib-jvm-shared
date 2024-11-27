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
package com.epam.drill.agent.test2code.common.transport

import kotlinx.serialization.Serializable
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.test2code.common.api.AstMethod

@Serializable
data class ClassMetadata(
    val groupId: String,
    val appId: String,
    val commitSha: String? = null,
    val buildVersion: String? = null,
    val instanceId: String,
    val methods: List<AstMethod>
): AgentMessage()