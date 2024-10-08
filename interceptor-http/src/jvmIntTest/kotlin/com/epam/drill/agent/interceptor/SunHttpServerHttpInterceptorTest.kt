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
package com.epam.drill.agent.interceptor

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

class SunHttpServerHttpInterceptorTest : AbstractHttpInterceptorTest() {

    override fun withHttpServer(block: (String) -> Unit) = HttpServer.create().run {
        try {
            this.bind(InetSocketAddress(0), 0)
            this.createContext("/", ::testRequestHandler)
            this.start()
            block("http://localhost:${this.address.port}")
        } finally {
            this.stop(2)
        }
    }

    private fun testRequestHandler(exchange: HttpExchange) {
        val requestBody = exchange.requestBody.readBytes()
        exchange.sendResponseHeaders(200, requestBody.size.toLong())
        exchange.responseBody.write(requestBody)
        exchange.responseBody.close()
    }

}
