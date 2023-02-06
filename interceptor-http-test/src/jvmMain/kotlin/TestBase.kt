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
import bindings.*
import java.io.*
import java.net.*
import kotlin.test.*

abstract class TestBase {
    var port: Int = 0

    @BeforeTest
    abstract fun setupServer()

    @Test
    fun `should add and remove hooks`() {
        val address = "http://localhost:$port"
        doHttpCall(address)
        Bindings.addHttpHook()
        val (key, value) = injectedHeaders.asSequence().first()

        doHttpCall(address).let { (headers, body) ->
            assertEquals(responseMessage, body.trim())
            assertEquals(value, headers[key]?.first())
        }

        Bindings.removeHttpHook()
        doHttpCall(address).let { (headers, body) ->
            assertEquals(responseMessage, body.trim())
            assertEquals(null, headers[key]?.first())
        }
    }

}

fun doHttpCall(address: String): Pair<MutableMap<String, MutableList<String>>, String> {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(address)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Language", "en-US")
        connection.useCaches = false
        connection.doOutput = true
        val wr = DataOutputStream(connection.outputStream)
        wr.close()

        val `is` = connection.inputStream
        val rd = BufferedReader(InputStreamReader(`is`))
        val response = StringBuilder() // or StringBuffer if Java version 5+
        var line: String?
        while (rd.readLine().also { line = it } != null) {
            response.append(line)
            response.append('\r')
        }
        rd.close()
        return connection.headerFields to response.toString()
    } catch (e: Exception) {

        e.printStackTrace()
        fail()
    } finally {
        connection?.disconnect()
    }
}
