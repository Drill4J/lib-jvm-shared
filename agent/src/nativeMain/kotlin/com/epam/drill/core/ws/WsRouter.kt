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
import com.epam.drill.api.dto.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import mu.KotlinLogging
import mu.KotlinLoggingLevel
import mu.KotlinLoggingConfiguration

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.core.ws.WsRouter")

@SharedImmutable
private val loader = Worker.start(true)

fun topicRegister() =
    WsRouter {
        WsRouter.inners("/agent/load").withPluginTopic { pluginMeta ->
            if (pstorage[pluginMeta.id] != null) {
                pluginMeta.sendPluginLoaded()
                logger.info { "Plugin '${pluginMeta.id}' is already loaded" }
                return@withPluginTopic
            }
            addPluginConfig(pluginMeta)
            loader.execute(
                TransferMode.UNSAFE,
                { pluginMeta }) { plugMessage ->
                logger.info { "try to load ${plugMessage.id} plugin" }
                val id = plugMessage.id
                agentConfig = agentConfig.copy(needSync = false)
                runBlocking {
                    loadPlugin(plugMessage)
                }
                plugMessage.sendPluginLoaded()
                logger.info { "$id plugin loaded" }
            }

        }

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

        //todo what is the use-case? change admin port when use https on admin?
        rawTopic<ServiceConfig>("/agent/update-config") { sc ->
            logger.info { "Agent got a system config: $sc" }
            secureAdminAddress = adminAddress?.copy(scheme = "https", defaultPort = sc.sslPort.toInt())
        }

        rawTopic("/agent/change-header-name") { headerName ->
            logger.info { "Agent got a new headerMapping: $headerName" }
            requestPattern = headerName.ifEmpty { null }
        }

        rawTopic<PackagesPrefixes>("/agent/set-packages-prefixes") { payload ->
            setPackagesPrefixes(payload)
            logger.info { "Agent packages prefixes have been changed" }
        }

        rawTopic("/agent/unload") { pluginId ->
            logger.warn { "Unload event. Plugin id is $pluginId" }
            PluginManager[pluginId]?.unload(UnloadReason.ACTION_FROM_ADMIN)
            println(
                """
                    |________________________________________________________
                    |Physical Deletion is not implemented yet.
                    |We should unload all resource e.g. classes, jars, .so/.dll
                    |Try to create custom classLoader. After this full GC.
                    |________________________________________________________
                """.trimMargin()
            )
        }

        rawTopic<PluginConfig>("/plugin/updatePluginConfig") { config ->
            logger.info { "UpdatePluginConfig event: message is $config " }
            val agentPluginPart = PluginManager[config.id]
            if (agentPluginPart != null) {
                agentPluginPart.off()
                agentPluginPart.updateRawConfig(config.data)
                agentPluginPart.on()
                logger.debug { "New settings for ${config.id} saved to file" }
            } else
                logger.warn { "Plugin ${config.id} not loaded to agent" }
        }

        rawTopic<PluginAction>("/plugin/action") { m ->
            logger.debug { "actionPlugin event: message is ${m.message} " }
            val agentPluginPart = PluginManager[m.id]
            agentPluginPart?.doRawAction(m.message)
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/plugin/action/${m.confirmationKey}"))
        }

    }

private fun PluginMetadata.sendPluginLoaded() {
    Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/$id/loaded"))
}
