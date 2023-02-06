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
import com.epam.drill.core.messanger.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import io.ktor.util.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

@SharedImmutable
private val tempTopicLogger = Logging.logger("tempTopicLogger")

@SharedImmutable
private val loader = Worker.start(true)

fun topicRegister() =
    WsRouter {
        WsRouter.inners("/agent/load").withPluginTopic { pluginMeta, file ->
            if (pstorage[pluginMeta.id] != null) {
                pluginMeta.sendPluginLoaded()
                tempTopicLogger.info { "Plugin '${pluginMeta.id}' is already loaded" }
                return@withPluginTopic
            }
            addPluginConfig(pluginMeta)
            loader.execute(
                TransferMode.UNSAFE,
                { pluginMeta to file }) { (plugMessage, file) ->
                tempTopicLogger.info { "try to load ${plugMessage.id} plugin" }
                val id = plugMessage.id
                agentConfig = agentConfig.copy(needSync = false)
                if (!plugMessage.isNative) runBlocking {
                    val path = generatePluginPath(id)
                    writeFileAsync(path, file)
                    loadPlugin(path, plugMessage)
                } else {
                    val natPlugin = generateNativePluginPath(id)

                    val dynamicLibrary = injectDynamicLibrary(natPlugin) as CPointed?

                    val loadedNativePlugin = nativePlugin(dynamicLibrary, id, staticCFunction(::sendNativeMessage))


                    loadedNativePlugin?.initPlugin()
                    loadedNativePlugin?.on()
                }
                plugMessage.sendPluginLoaded()
                tempTopicLogger.info { "$id plugin loaded" }
            }

        }

        rawTopic("/plugin/state") {
            tempTopicLogger.info { "Agent's plugin state sending triggered " }
            tempTopicLogger.info { "Plugins: ${pstorage.size}" }
            pstorage.onEach { (_, plugin) ->
                plugin.onConnect()
            }
        }

        rawTopic<LoggingConfig>("/agent/logging/update-config") { lc ->
            tempTopicLogger.info { "Agent got a logging config: $lc" }
            Logging.logLevel = when {
                lc.trace -> LogLevel.TRACE
                lc.debug -> LogLevel.DEBUG
                lc.info -> LogLevel.INFO
                lc.warn -> LogLevel.WARN
                else -> LogLevel.ERROR
            }
        }

        rawTopic<UpdateInfo>("/agent/update-parameters") { info ->
            val parameters = info.parameters
            tempTopicLogger.debug { "Agent update config by $parameters" }
            val newParameters = HashMap(agentConfig.parameters)
            parameters.forEach { updateParameter ->
                newParameters[updateParameter.key]?.let {
                    newParameters[updateParameter.key] = it.copy(value = updateParameter.value)
                } ?: tempTopicLogger.warn { "cannot find and update the parameter '$updateParameter'" }
            }
            agentConfig = agentConfig.copy(parameters = newParameters)
            agentConfigUpdater.updateParameters(agentConfig)
        }

        //todo what is the use-case? change admin port when use https on admin?
        rawTopic<ServiceConfig>("/agent/update-config") { sc ->
            tempTopicLogger.info { "Agent got a system config: $sc" }
            secureAdminAddress = adminAddress?.copy(scheme = "https", defaultPort = sc.sslPort.toInt())
        }

        rawTopic("/agent/change-header-name") { headerName ->
            tempTopicLogger.info { "Agent got a new headerMapping: $headerName" }
            requestPattern = if (headerName.isEmpty()) null else headerName
        }

        rawTopic<PackagesPrefixes>("/agent/set-packages-prefixes") { payload ->
            setPackagesPrefixes(payload)
            tempTopicLogger.info { "Agent packages prefixes have been changed" }
        }

        rawTopic("/agent/unload") { pluginId ->
            tempTopicLogger.warn { "Unload event. Plugin id is $pluginId" }
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
        rawTopic("/agent/load-classes-data") {
            Sender.send(Message(MessageType.START_CLASSES_TRANSFER, ""))
            try {
                val data = getClassesByConfig()
                Sender.send(Message(MessageType.CLASSES_DATA, "", data))
            } catch (ignored: Exception) {
                tempTopicLogger.warn { "can't process class message" }
            }
            Sender.send(Message(MessageType.FINISH_CLASSES_TRANSFER, ""))
            tempTopicLogger.info { "Agent's application classes processing by config triggered" }
        }

        rawTopic<PluginConfig>("/plugin/updatePluginConfig") { config ->
            tempTopicLogger.info { "UpdatePluginConfig event: message is $config " }
            val agentPluginPart = PluginManager[config.id]
            if (agentPluginPart != null) {
                agentPluginPart.setEnabled(false)
                agentPluginPart.off()
                agentPluginPart.updateRawConfig(config.data)
                agentPluginPart.setEnabled(true)
                agentPluginPart.on()
                tempTopicLogger.debug { "New settings for ${config.id} saved to file" }
            } else
                tempTopicLogger.warn { "Plugin ${config.id} not loaded to agent" }
        }

        rawTopic<PluginAction>("/plugin/action") { m ->
            tempTopicLogger.debug { "actionPlugin event: message is ${m.message} " }
            val agentPluginPart = PluginManager[m.id]
            agentPluginPart?.doRawAction(m.message)
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/plugin/action/${m.confirmationKey}"))
        }

        rawTopic<TogglePayload>("/plugin/togglePlugin") { (pluginId, forceValue) ->
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart == null) {
                tempTopicLogger.warn { "Plugin $pluginId not loaded to agent" }
            } else {
                tempTopicLogger.info { "togglePlugin event: PluginId is $pluginId" }
                val newValue = forceValue ?: !agentPluginPart.isEnabled()
                agentPluginPart.setEnabled(newValue)
                if (newValue) agentPluginPart.on() else agentPluginPart.off()
            }
            sendPluginToggle(pluginId)
        }
    }

private fun PluginMetadata.sendPluginLoaded() {
    Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/$id/loaded"))
}

private fun sendPluginToggle(pluginId: String) {
    Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/${pluginId}/toggle"))
}

private fun generateNativePluginPath(id: String): String {
    //fixme do generate Native path
    return "$id/native_plugin.os_lib"
}

private fun generatePluginPath(id: String): String {
    val ajar = "agent-part.jar"
    val pluginsDir = "${if (tempPath.isEmpty()) drillInstallationDir else tempPath}/drill-plugins"
    doMkdir(pluginsDir)
    var pluginDir = "$pluginsDir/$id"
    doMkdir(pluginDir)
    pluginDir = "$pluginDir/${agentConfig.id}"
    doMkdir(pluginDir)
    val path = "$pluginDir/$ajar"
    return path
}
