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

import java.net.HttpURLConnection
import java.net.URL

class JavaHttpClientTransformerObjectTest : AbstractClientTransformerObjectTest() {

    override fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String>,
        request: String
    ): Pair<Map<String, String>, String> {
        lateinit var connection: HttpURLConnection
        try {
            connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            headers.entries.forEach {
                connection.setRequestProperty(it.key, it.value)
            }
            connection.doOutput = true
            connection.outputStream.write(request.encodeToByteArray())
            connection.outputStream.close()
            val responseHeaders = connection.headerFields.mapValues { it.value.joinToString(",") }
            val responseBody = connection.inputStream.readBytes().decodeToString()
            connection.inputStream.close()
            return responseHeaders to responseBody
        } finally {
            connection.disconnect()
        }
    }

}
