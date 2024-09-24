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
package com.epam.drill.agent.instrument.jetty

import java.net.URI
import javax.websocket.ClientEndpointConfig
import org.eclipse.jetty.websocket.jsr356.ClientContainer
import com.epam.drill.agent.instrument.servers.AbstractWsMessagesTransformerObjectTest

class JettyWsMessagesTransformerObjectTest : AbstractWsMessagesTransformerObjectTest() {

    override fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit) = JettyWsTestServer.withWebSocketEndpoint(
        JettyWsTestServer.AnnotatedEndpointConfigurator(TestRequestServerAnnotatedEndpoint::class.java),
        block
    )

    override fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit) = JettyWsTestServer.withWebSocketEndpoint(
        JettyWsTestServer.InterfaceEndpointConfigurator(TestRequestServerInterfaceEndpoint::class.java),
        block
    )

    override fun connectToWebsocketAnnotatedEndpoint(address: String) = TestRequestClientAnnotatedEndpoint().run {
        val session = ClientContainer().also(ClientContainer::start).connectToServer(this, URI(address))
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(address: String) = TestRequestClientInterfaceEndpoint().run {
        val session = ClientContainer().also(ClientContainer::start)
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(address))
        this to session
    }

}
