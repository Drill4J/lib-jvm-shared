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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.net.InetSocketAddress
import java.util.Objects
import org.simpleframework.http.Request
import org.simpleframework.http.Response
import org.simpleframework.http.Status
import org.simpleframework.http.core.Container
import org.simpleframework.http.core.ContainerSocketProcessor
import org.simpleframework.transport.connect.SocketConnection
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.common.agent.request.DrillRequest

@Suppress("FunctionName")
abstract class AbstractClientTransformerObjectTest {

    @Test
    fun `test request-response with empty thread session and headers data`() = withHttpServer(returnHeaders = true) {
        TestRequestHolder.remove()
        val response = callHttpEndpoint(it)
        val responseHeaders = response.first
        val responseBody = response.second
        val drillHeaders = responseHeaders.filterKeys(Objects::nonNull).filterKeys { it.startsWith("drill-") }
        assertNull(TestRequestHolder.retrieve())
        assertTrue(drillHeaders.isEmpty())
        assertEquals("test-request", responseBody)
    }

    @Test
    fun `test request with existing thread session data`() = withHttpServer(returnHeaders = true) {
        TestRequestHolder.store(DrillRequest("session-123", mapOf("drill-header-data" to "test-data")))
        val response = callHttpEndpoint(it)
        val responseHeaders = response.first
        val responseBody = response.second
        assertEquals("session-123-returned", responseHeaders["drill-session-id"])
        assertEquals("test-data-returned", responseHeaders["drill-header-data"])
        assertEquals("test-request", responseBody)
    }

    @Test
    fun `test response with existing headers data`() = withHttpServer(produceHeaders = true) {
        TestRequestHolder.remove()
        val response = callHttpEndpoint(it)
        val responseBody = response.second
        val drillRequest = TestRequestHolder.retrieve()!!
        assertEquals("session-123-produced", drillRequest.drillSessionId)
        assertEquals("test-data-produced", drillRequest.headers.get("drill-header-data"))
        assertEquals("test-request", responseBody)
    }

    protected abstract fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        request: String = "test-request"
    ): Pair<Map<String, String>, String>

    private fun withHttpServer(
        returnHeaders: Boolean = false,
        produceHeaders: Boolean = false,
        block: (String) -> Unit
    ) = SocketConnection(ContainerSocketProcessor(TestContainer(returnHeaders, produceHeaders))).use {
        val address = it.connect(InetSocketAddress(0)) as InetSocketAddress
        block("http://localhost:${address.port}")
    }

    private class TestContainer(
        private val returnHeaders: Boolean,
        private val produceHeaders: Boolean
    ): Container {
        override fun handle(request: Request, response: Response) {
            if (returnHeaders) {
                request.header.lines().drop(1)
                    .associate { it.substringBefore(":").trim() to it.substringAfter(":", "").trim() }
                    .filterKeys { it.startsWith("drill-") }
                    .forEach { response.setValue(it.key, "${it.value}-returned") }
            }
            if (produceHeaders) {
                response.setValue("drill-session-id", "session-123-produced")
                response.setValue("drill-header-data", "test-data-produced")
            }
            response.status = Status.OK
            response.outputStream.write(request.inputStream.readBytes())
            response.outputStream.close()
        }
    }

}
