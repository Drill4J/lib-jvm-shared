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
package com.epam.drill.plugin.api.end

suspend fun AdminPluginPart<*>.sendToAgent(
    buildVersion: String = agentInfo.buildVersion,
    destination: Any,
    message: Any,
) = sender.send(
    context = AgentSendContext(id, buildVersion),
    destination = destination,
    message = message,
)

suspend fun AdminPluginPart<*>.sendToGroup(destination: Any, message: Any) = sender.send(
    context = GroupSendContext(agentInfo.serviceGroup),
    destination = destination,
    message = message,
)

data class AgentSendContext(
    val agentId: String,
    val buildVersion: String,
    val filterId: String = "",
) : SendContext

data class GroupSendContext(
    val groupId: String,
) : SendContext

@Deprecated(
    message = "This method is no longer acceptable",
    replaceWith = ReplaceWith("AdminPluginPart<*>.sendToAgent(...)")
)
suspend fun Sender.send(
    agentId: String,
    buildVersion: String,
    filterId: String = "",
    destination: Any,
    message: Any,
) = send(AgentSendContext(agentId, buildVersion, filterId), destination, message)
