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
package com.epam.drill.agent.transport

import kotlin.concurrent.thread
import java.io.File
import org.apache.hc.core5.http.HttpStatus
import mu.KotlinLogging
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.agent.transport.ResponseStatus

object HttpAgentMessageSender : AgentMessageSender {

    private val logger = KotlinLogging.logger {}

    private var attached = false
    private val agentId = WsConfiguration.getAgentId()
    private val buildVersion = WsConfiguration.getBuildVersion()

    override fun isTransportAvailable(): Boolean = attached

    override fun <T : AgentMessage> send(destination: AgentMessageDestination, message: T): ResponseStatus {
        val status = when (destination.type) {
            "POST" -> HttpClient.post("agents/$agentId/builds/$buildVersion/${destination.target}", message)
            "PUT" -> HttpClient.put("agents/$agentId/builds/$buildVersion/${destination.target}", message)
            else -> -1
        }
        return HttpResponseStatus(status)
    }

    @Suppress("UNUSED")
    fun sendAgentInstance() {
        thread {
            HttpClient.configure(
                WsConfiguration.getAdminAddress(),
                checkSslTruststorePath(WsConfiguration.getSslTruststore()),
                WsConfiguration.getSslTruststorePassword()
            )
            val agentConfigHex = WsConfiguration.getAgentConfigHexString()
            val httpCall: (String) -> Int = { HttpClient.put("agents", it) }
            val logError: (Throwable) -> Unit = { logger.error(it) { "sendAgentInstance: Attempt is failed: $it" } }
            var status = 0
            logger.debug { "agentAttach: Sending request to admin server" }
            while (status != HttpStatus.SC_OK) {
                status = agentConfigHex.runCatching(httpCall).onFailure(logError).getOrDefault(0)
                if (status != HttpStatus.SC_OK) Thread.sleep(5000)
            }
            logger.debug { "sendAgentInstance: Sending request to admin server: successful" }
            attached = true
        }
    }

    private fun checkSslTruststorePath(filePath: String) = File(filePath).run {
        val drillPath = WsConfiguration.getDrillInstallationDir()
            .removeSuffix(File.pathSeparator)
            .takeIf(String::isNotEmpty)
            ?: "."
        this.takeIf(File::exists)?.let(File::getAbsolutePath)
            ?: this.takeUnless(File::isAbsolute)?.let { File(drillPath).resolve(it).absolutePath }
            ?: filePath
    }

}
