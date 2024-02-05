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
package com.epam.drill.agent.instrument.clients

import kotlin.test.BeforeTest
import java.net.InetSocketAddress
import javax.net.ssl.HttpsURLConnection
import org.simpleframework.http.core.ContainerSocketProcessor
import org.simpleframework.transport.connect.SocketConnection
import com.epam.drill.agent.instrument.TestSslContextFactory

class JavaHttpsClientTransformerObjectTest : JavaHttpClientTransformerObjectTest() {

    @BeforeTest
    fun configureClientTrustAll() =
        HttpsURLConnection.setDefaultSSLSocketFactory(TestSslContextFactory.trustAllContext().socketFactory)

    override fun withHttpServer(
        returnHeaders: Boolean,
        produceHeaders: Boolean,
        block: (String) -> Unit
    ) = SocketConnection(ContainerSocketProcessor(TestRequestContainer(returnHeaders, produceHeaders))).use {
        val address = it.connect(InetSocketAddress(0), TestSslContextFactory.testServerContext()) as InetSocketAddress
        block("https://localhost:${address.port}")
    }

}
