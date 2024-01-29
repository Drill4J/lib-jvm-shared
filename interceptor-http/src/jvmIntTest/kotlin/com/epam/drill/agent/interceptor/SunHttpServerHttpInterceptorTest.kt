package com.epam.drill.agent.interceptor

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer

class SunHttpServerHttpInterceptorTest : AbstractHttpInterceptorTest() {

    override fun withHttpServer(block: (String) -> Unit) = HttpServer.create().run {
        try {
            this.bind(InetSocketAddress(0), 0)
            this.createContext("/") { exchange ->
                val requestBody = exchange.requestBody.readBytes()
                exchange.sendResponseHeaders(200, requestBody.size.toLong())
                exchange.responseBody.write(requestBody)
                exchange.responseBody.close()
            }
            this.start()
            block("http://localhost:${this.address.port}")
        } finally {
            this.stop(2)
        }
    }

}
