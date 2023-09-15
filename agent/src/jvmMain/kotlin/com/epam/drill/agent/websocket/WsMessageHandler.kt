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
package com.epam.drill.agent.websocket

import kotlin.time.measureTime
import kotlinx.serialization.serializer
import javax.websocket.MessageHandler
import mu.KotlinLogging
import com.epam.drill.agent.PluginStorage
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.agent.websocket.topic.GenericTopic
import com.epam.drill.agent.websocket.topic.InfoTopic
import com.epam.drill.agent.websocket.topic.Topic
import com.epam.drill.common.agent.configuration.PackagesPrefixes
import com.epam.drill.common.message.Message
import com.epam.drill.common.message.MessageType
import com.epam.drill.common.ws.dto.PluginAction
import com.epam.drill.common.ws.dto.TogglePayload

object WsMessageHandler : MessageHandler.Whole<Message> {

    private val logger = KotlinLogging.logger {}
    private val topics = mutableMapOf<String, Topic>()

    override fun onMessage(message: Message) {
        val destination = message.destination
        val topic = topics[destination]
        when (topic) {
            is InfoTopic -> {
                val duration = measureTime { topic.run(message.data) }
                logger.trace { "onMessage[InfoTopic]: Message processed, destination=$destination, duration=$duration" }
                WsClient.sendMessage(Message(MessageType.MESSAGE_DELIVERED, destination))
            }
            is GenericTopic<*> -> {
                val duration = measureTime { topic.deserializeAndRun(message.data) }
                logger.trace { "onMessage[GenericTopic]: Message processed, destination=$destination, duration=$duration" }
                WsClient.sendMessage(Message(MessageType.MESSAGE_DELIVERED, destination))
            }
            null -> logger.warn { "onMessage: Destination not registered, destination=$destination" }
            else -> logger.warn { "onMessage: Topic class unknown, destination=$destination, topic.class=${topic::class}" }
        }
    }

    fun registerTopics() {
        infoTopic("/agent/change-header-name") { headerName ->
            logger.info { "infoTopic[/agent/change-header-name]: Received headerName=$headerName" }
            WsConfiguration.setRequestPattern(headerName.ifEmpty { null })
        }
        genericTopic<PackagesPrefixes>("/agent/set-packages-prefixes") { prefixes ->
            logger.info { "genericTopic[/agent/set-packages-prefixes]: Received prefixes=$prefixes" }
        }
        genericTopic<PluginAction>("/plugin/action") { msg ->
            logger.info { "genericTopic[/plugin/action]: Received pluginId=${msg.id}, message=${msg.message}" }
            val agentPluginPart = PluginStorage[msg.id]
            agentPluginPart?.doRawAction(msg.message)
            WsClient.sendMessage(Message(MessageType.MESSAGE_DELIVERED, "/plugin/action/${msg.confirmationKey}"))
        }
        genericTopic<TogglePayload>("/plugin/togglePlugin") { (pluginId, forceValue) ->
            logger.info { "genericTopic[/plugin/togglePlugin]: Received pluginId=$pluginId, forceValue=$forceValue" }
            val agentPlugin = PluginStorage[pluginId]
            if (agentPlugin == null) {
                logger.warn { "genericTopic[/plugin/togglePlugin]: Plugin not found, pluginId=$pluginId" }
            } else if (forceValue != false) {
                logger.info { "genericTopic[/plugin/togglePlugin]: Plugin toggled 'on', pluginId=$pluginId" }
                agentPlugin.on()
            }
            WsClient.sendMessage(Message(MessageType.MESSAGE_DELIVERED, "/agent/plugin/${pluginId}/toggle"))
        }
    }

    @Suppress("SameParameterValue")
    private fun infoTopic(destination: String, block: (String) -> Unit) {
        val topic = InfoTopic(destination, block)
        topics.put(destination, topic)
    }

    private inline fun <reified T : Any> genericTopic(destination: String, noinline block: (T) -> Unit) {
        val topic = GenericTopic(destination, T::class.serializer(), block)
        topics.put(destination, topic)
    }

}
