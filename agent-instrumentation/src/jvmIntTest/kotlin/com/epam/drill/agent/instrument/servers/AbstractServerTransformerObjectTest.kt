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
package com.epam.drill.agent.instrument.servers

import kotlin.test.Test
import kotlin.test.assertEquals
import java.net.HttpURLConnection
import java.net.URL
import java.util.Objects
import mu.KLogger
import com.epam.drill.agent.instrument.TestRequestHolder

@Suppress("FunctionName")
abstract class AbstractServerTransformerObjectTest {

    protected abstract val logger: KLogger

    @Test
    fun `test with empty headers request`() = withHttpServer {
        TestRequestHolder.remove()
        val response = callHttpEndpoint(it)
        val responseHeaders = response.first
        val responseBody = response.second
        val drillHeaders = responseHeaders.filterKeys(Objects::nonNull).filterKeys { it.startsWith("drill-") }
        assertEquals(2, drillHeaders.size)
        assertEquals("test-agent", responseHeaders["drill-agent-id"])
        assertEquals("test-admin:8080", responseHeaders["drill-admin-url"])
        assertEquals("test-request", responseBody)
    }

    @Test
    fun `test with session headers request`() = withHttpServer {
        TestRequestHolder.remove()
        val requestHeaders = mapOf(
            "drill-session-id" to "session-123",
            "drill-header-data" to "test-data"
        )
        val response = callHttpEndpoint(it, requestHeaders)
        val responseHeaders = response.first
        val responseBody = response.second
        assertEquals("test-agent", responseHeaders["drill-agent-id"])
        assertEquals("test-admin:8080", responseHeaders["drill-admin-url"])
        assertEquals("session-123", responseHeaders["drill-session-id"])
        assertEquals("test-data", responseHeaders["drill-header-data"])
        assertEquals("test-request", responseBody)
    }

    protected abstract fun withHttpServer(block: (String) -> Unit)

    private fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String = "plain/text",
        body: String = "test-request"
    ): Pair<Map<String, String>, String> {
        lateinit var connection: HttpURLConnection
        try {
            logger.trace { "callHttpEndpoint: Requesting $endpoint: headers=$headers, body=$body" }
            connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", contentType)
            headers.entries.forEach {
                connection.setRequestProperty(it.key, it.value)
            }
            connection.doOutput = true
            connection.outputStream.write(body.encodeToByteArray())
            connection.outputStream.close()
            val responseHeaders = connection.headerFields.mapValues { it.value.joinToString(",") }
            val responseBody = connection.inputStream.readBytes().decodeToString()
            connection.inputStream.close()
            logger.trace { "callHttpEndpoint: Response from $endpoint: headers=$responseHeaders, body=$responseBody" }
            return responseHeaders to responseBody
        } finally {
            connection.disconnect()
        }
    }

}
