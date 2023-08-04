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
package com.epam.drill.core.ws

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.message.*
import com.epam.drill.common.ws.dto.*
import com.epam.drill.core.*
import com.epam.drill.plugin.*
import kotlin.native.concurrent.*
import mu.KotlinLogging
import mu.KotlinLoggingLevel
import mu.KotlinLoggingConfiguration

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.core.ws.WsRouter")

fun topicRegister() =
    WsRouter {
        rawTopic("/plugin/state") {
            logger.info { "Agent's plugin state sending triggered " }
            logger.info { "Plugins: ${pstorage.size}" }
            pstorage.onEach { (_, plugin) ->
                plugin.onConnect()
            }
        }

        rawTopic<LoggingConfig>("/agent/logging/update-config") { lc ->
            logger.info { "Agent got a logging config: $lc" }
            KotlinLoggingConfiguration.logLevel = when {
                lc.trace -> KotlinLoggingLevel.TRACE
                lc.debug -> KotlinLoggingLevel.DEBUG
                lc.info -> KotlinLoggingLevel.INFO
                lc.warn -> KotlinLoggingLevel.WARN
                else -> KotlinLoggingLevel.ERROR
            }
        }

        rawTopic<UpdateInfo>("/agent/update-parameters") { info ->
            val parameters = info.parameters
            logger.debug { "Agent update config by $parameters" }
            val newParameters = HashMap(agentConfig.parameters)
            parameters.forEach { updateParameter ->
                newParameters[updateParameter.key]?.let {
                    newParameters[updateParameter.key] = it.copy(value = updateParameter.value)
                } ?: logger.warn { "cannot find and update the parameter '$updateParameter'" }
            }
            agentConfig = agentConfig.copy(parameters = newParameters)
            agentConfigUpdater.updateParameters(agentConfig)
        }

        rawTopic("/agent/change-header-name") { headerName ->
            logger.info { "Agent got a new headerMapping: $headerName" }
            requestPattern = headerName.ifEmpty { null }
        }

        rawTopic<PackagesPrefixes>("/agent/set-packages-prefixes") { payload ->
            setPackagesPrefixes(payload)
            logger.info { "Agent packages prefixes have been changed" }
        }

        rawTopic<PluginAction>("/plugin/action") { m ->
            logger.debug { "actionPlugin event: message is ${m.message} " }
            val agentPluginPart = PluginManager[m.id]
            agentPluginPart?.doRawAction(m.message)
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/plugin/action/${m.confirmationKey}"))
        }

        rawTopic<TogglePayload>("/plugin/togglePlugin") { (pluginId, forceValue) ->
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart == null) {
                logger.warn { "Plugin $pluginId not loaded to agent" }
            } else {
                logger.info { "togglePlugin event: PluginId is $pluginId" }
                if (forceValue != false) agentPluginPart.on()
            }
            sendPluginToggle(pluginId)
        }

    }

private fun sendPluginToggle(pluginId: String) {
    Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/${pluginId}/toggle"))
}
