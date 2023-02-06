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
package io.ktor.samples.http2push


import bindings.Bindings
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.junit.Before
import org.junit.Test
import java.io.File
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.jetty.Jetty
import io.ktor.client.request.request
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.html.*
import org.eclipse.jetty.util.ssl.SslContextFactory
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.toJavaDuration


class JettyTest {
    var port: Int = 0

    @Test
    fun xx(
    ) {
                Bindings.addHttpHook()
        runBlocking {
            val client = HttpClient(Jetty) {
                engine {
                    sslContextFactory = SslContextFactory(true)
                }
            }
            val request = client.request<String>("https://localhost:8443") {
                this.method = HttpMethod.Get

            }

            println(request)

        }
    }


    @Before
    fun setupServer() {
//        Bindings.addHttpHook()
        val file = File("build/temporary.jks")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            generateCertificate(file)
        }
        // run embedded server
        val embeddedServer = embeddedServer(Netty, commandLineEnvironment(emptyArray()))
        embeddedServer.start(
        )
    }
}


fun Application.main() {
    Bindings.addHttpHook()
    install(DefaultHeaders)
    install(CallLogging)
    routing {
        get("/") {
            call.push("/style.css")
            call.respondHtml {
                head {
                    title { +"Ktor: http2-push" }
                    styleLink("/style.css")
                }
                body {
                    p {
                        +"Hello from Ktor HTTP/2 push sample application"
                    }
                }
            }
        }
        get("/style.css") {
            call.respondText("p { color: blue }", contentType = ContentType.Text.CSS)
        }
    }
}