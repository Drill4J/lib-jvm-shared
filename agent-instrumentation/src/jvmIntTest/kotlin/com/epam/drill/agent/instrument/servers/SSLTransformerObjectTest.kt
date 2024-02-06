package com.epam.drill.agent.instrument.servers

import kotlin.test.BeforeTest
import java.net.InetSocketAddress
import javax.net.ssl.HttpsURLConnection
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import mu.KLogger
import mu.KotlinLogging
import com.epam.drill.agent.instrument.TestSslContextFactory

class SSLTransformerObjectTest : AbstractServerTransformerObjectTest() {

    override val logger: KLogger = KotlinLogging.logger {}

    @BeforeTest
    fun configureClientTrustAll() =
        HttpsURLConnection.setDefaultSSLSocketFactory(TestSslContextFactory.trustAllContext().socketFactory)

    override fun withHttpServer(block: (String) -> Unit) = HttpsServer.create().run {
        try {
            this.httpsConfigurator = HttpsConfigurator(TestSslContextFactory.testServerContext())
            this.bind(InetSocketAddress(0), 0)
            this.createContext("/", ::testRequestHandler)
            this.start()
            block("https://localhost:${this.address.port}")
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
