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

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

class OkHttp3ClientTransformerObjectTest : AbstractClientTransformerObjectTest() {

    override fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String>,
        body: String
    ): Pair<Map<String, String>, String> {
        lateinit var response: Response
        try {
            val requestBuilder = Request.Builder().url(endpoint)
            headers.entries.forEach {
                requestBuilder.addHeader(it.key, it.value)
            }
            requestBuilder.post(RequestBody.create(MediaType.get("text/text"), body))
            response = OkHttpClient().newCall(requestBuilder.build()).execute()
            val responseHeaders = response.headers().toMultimap().mapValues { it.value.joinToString(",") }
            val responseBody = response.body()!!.string()
            return responseHeaders to responseBody
        } finally {
            response.close()
        }
    }

}
