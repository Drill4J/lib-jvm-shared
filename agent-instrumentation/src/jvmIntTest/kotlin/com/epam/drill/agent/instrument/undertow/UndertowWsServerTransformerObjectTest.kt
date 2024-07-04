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
package com.epam.drill.agent.instrument.undertow

import java.net.InetSocketAddress
import javax.websocket.server.ServerEndpointConfig
import org.xnio.OptionMap
import org.xnio.Xnio
import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentManager
import io.undertow.websockets.jsr.WebSocketDeploymentInfo
import mu.KotlinLogging
import com.epam.drill.agent.instrument.servers.AbstractWsServerTransformerObjectTest

class UndertowWsServerTransformerObjectTest : AbstractWsServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(TestRequestServerAnnotatedEndpoint::class.java)
        .let { withWebSocketEndpoint(it, block) }

    override fun withWebSocketInterfaceEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(ServerEndpointConfig.Builder.create(TestRequestServerInterfaceEndpoint::class.java, "/").build())
        .let { withWebSocketEndpoint(it, block) }

    private fun withWebSocketEndpoint(info: WebSocketDeploymentInfo, block: (String) -> Unit) = Undertow.builder().run {
        val wsDeploymentManager = webSocketDeploymentManager(info)
        this.addHttpListener(0, "localhost")
        this.setHandler(wsDeploymentManager.also(DeploymentManager::deploy).let(DeploymentManager::start))
        val server = this.build()
        try {
            server.start()
            block("ws://localhost:${(server.listenerInfo[0].address as InetSocketAddress).port}")
        } finally {
            wsDeploymentManager.stop()
            wsDeploymentManager.undeploy()
            server.stop()
        }
    }

    private fun webSocketDeploymentManager(info: WebSocketDeploymentInfo) = Servlets.defaultContainer().run {
        val worker = Xnio.getInstance().createWorker(OptionMap.EMPTY)
        val deployment = Servlets.deployment()
            .setClassLoader(this::class.java.classLoader)
            .setContextPath("/")
            .setDeploymentName("test-websockets")
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info.setWorker(worker))
        this.addDeployment(deployment)
    }

}
