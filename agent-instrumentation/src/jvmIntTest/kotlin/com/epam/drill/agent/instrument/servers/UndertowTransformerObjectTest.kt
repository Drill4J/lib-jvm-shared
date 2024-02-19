package com.epam.drill.agent.instrument.servers

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import mu.KotlinLogging
import java.net.InetSocketAddress

class UndertowTransformerObjectTest : AbstractServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withHttpServer(block: (String) -> Unit) = Undertow.builder()
        .addHttpListener(0, "localhost")
        .setHandler(::testRequestHandler)
        .build().run {
            try {
                this.start()
                block("http://localhost:${(this.listenerInfo[0].address as InetSocketAddress).port}")
            } finally {
                this.stop()
            }
        }

    private fun testRequestHandler(exchange: HttpServerExchange) {
        lateinit var requestBody: ByteArray
        exchange.requestReceiver.receiveFullBytes { _, body -> requestBody = body }
        exchange.statusCode = 200
        exchange.responseContentLength = requestBody.size.toLong()
        exchange.responseSender.send(requestBody.decodeToString())
        exchange.responseSender.close()
    }
}
