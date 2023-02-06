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
import com.epam.drill.transport.WSClientFactory
import com.epam.drill.transport.common.ws.URL
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class MainTest {
    private val iterations = 100
    private val messageSize = 100_000

    @Test
    @Ignore
    fun tst() = runBlocking {
        val atomic = atomic(0)
        val isOpened = atomic(false)
        val message = ByteArray(messageSize) { 'c'.code.toByte() }
        //TODO ws://echo.websocket.org/echo is not available EPMDJ-8604
        val client = WSClientFactory.createClient(
            URL("ws://echo.websocket.org/echo"),
            txBufferSize = messageSize,
            rxBufferSize = messageSize
        )

        client.onOpen {
            println("Opened")
            isOpened.value = true
        }

        client.onBinaryMessage {
            atomic.incrementAndGet()
        }

        client.onError {
            println("Error!")
        }

        client.onClose {
            println("Closed!")
        }

        while (!isOpened.value) {
            delay(10)
        }

        repeat(iterations) {
            client.send(message)
        }
        delay(20_000)
        assertEquals(iterations, atomic.value)
    }
}


