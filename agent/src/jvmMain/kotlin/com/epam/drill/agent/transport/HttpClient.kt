package com.epam.drill.agent.transport

import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import java.net.URI
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.io.entity.StringEntity
import mu.KotlinLogging
import com.epam.drill.common.agent.transport.AgentMessage

object HttpClient {

    private val logger = KotlinLogging.logger {}

    private lateinit var adminUri: URI

    fun configure(adminAddress: String, sslTruststore: String, sslTruststorePass: String) {
        adminUri = URI(adminAddress)
        logger.debug { "configure: Using adminAddress: ${adminUri}" }
        if(adminUri.scheme == "https") {
            logger.debug { "configure: SSL configured, trustAll: " }
            logger.debug { "configure: SSL configured, truststore: " }
        }
    }

    fun post(path: String, entity: AgentMessage): Int = execute(HttpPost(adminUri.resolve(path)), entity)

    fun put(path: String, entity: AgentMessage): Int = execute(HttpPut(adminUri.resolve(path)), entity)

    fun put(path: String, entity: String): Int = execute(HttpPut(adminUri.resolve(path)), entity)

    private fun execute(request: ClassicHttpRequest, entity: AgentMessage) = HttpClients.createDefault().use {
        request.setHeader("Content-Type", "application/x-protobuf")
        request.entity = StringEntity(ProtoBuf.encodeToHexString(entity))
        logger.trace { "execute: Request to ${request.uri}, method: ${request.method}, message=$entity" }
        it.execute(request, ::statusResponseHandler)!!
    }

    private fun execute(request: ClassicHttpRequest, entity: String) = HttpClients.createDefault().use {
        request.setHeader("Content-Type", "application/x-protobuf")
        request.entity = StringEntity(entity)
        logger.trace { "execute: Request to ${request.uri}, method: ${request.method}, message=$entity" }
        it.execute(request, ::statusResponseHandler)!!
    }

    private fun statusResponseHandler(response: ClassicHttpResponse): Int = response.code

}
