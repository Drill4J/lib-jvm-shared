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
package com.epam.drill.agent.ws

import com.epam.drill.agent.*
import com.epam.drill.agent.configuration.*
import com.epam.drill.agent.ws.send.*
import com.epam.drill.common.agent.configuration.*
import com.epam.drill.common.message.*
import com.epam.drill.common.ws.dto.*
import kotlin.native.concurrent.*
import mu.KotlinLogging

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.ws.WsTopicRegister")

fun wsTopicRegister() =
    WsRouter {
        rawTopic("/agent/change-header-name") { headerName ->
            logger.info { "Agent got a new headerMapping: $headerName" }
            requestPattern = headerName.ifEmpty { null }
        }
        rawTopic<PackagesPrefixes>("/agent/set-packages-prefixes") { payload ->
            logger.info { "Agent packages prefixes have been changed" }
        }
        rawTopic<PluginAction>("/plugin/action") { m ->
            logger.debug { "actionPlugin event: message is ${m.message} " }
            val agentPluginPart = PluginStorage[m.id]
            agentPluginPart?.doRawAction(m.message)
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/plugin/action/${m.confirmationKey}"))
        }
        rawTopic<TogglePayload>("/plugin/togglePlugin") { (pluginId, forceValue) ->
            val agentPluginPart = PluginStorage[pluginId]
            if (agentPluginPart == null) {
                logger.warn { "Plugin $pluginId not loaded to agent" }
            } else {
                logger.info { "togglePlugin event: PluginId is $pluginId" }
                if (forceValue != false) agentPluginPart.on()
            }
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/${pluginId}/toggle"))
        }
    }
