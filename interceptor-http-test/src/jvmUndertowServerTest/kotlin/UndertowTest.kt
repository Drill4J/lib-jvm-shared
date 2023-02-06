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
import io.undertow.*
import io.undertow.util.*
import java.net.*

class UndertowTest : TestBase() {

    override fun setupServer() {
        val server = Undertow.builder()
            .addHttpListener(0, "localhost")
            .setHandler { exchange ->
                exchange.responseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
                exchange.responseSender.send(responseMessage)
            }.build()
        server.start()
        port = (server.listenerInfo.first().address as InetSocketAddress).port
    }

}
