package com.epam.drill.agent.transport.http

import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.ResponseContent
import com.epam.drill.agent.common.transport.ResponseStatus
import com.epam.drill.agent.transport.AgentMessageDeserializer
import com.epam.drill.agent.transport.AgentMessageSerializer
import com.epam.drill.agent.transport.AgentMessageTransport

class TypedHttpAgentMessageTransport<T : AgentMessage, R>(
    serverAddress: String,
    apiKey: String = "",
    sslTruststore: String = "",
    sslTruststorePass: String = "",
    drillInternal: Boolean = true,
    gzipCompression: Boolean = true,
    receiveContent: Boolean = false,
    private val messageSerializer: AgentMessageSerializer<T, ByteArray>,
    private val messageDeserializer: AgentMessageDeserializer<ByteArray, R>
) : AgentMessageTransport<T, R> {
    private val delegate: AgentMessageTransport<ByteArray, ByteArray> = HttpAgentMessageTransport(
        serverAddress,
        apiKey,
        sslTruststore,
        sslTruststorePass,
        drillInternal,
        gzipCompression,
        receiveContent
    )

    override fun send(destination: AgentMessageDestination, message: T?, contentType: String): ResponseStatus =
        delegate.send(
            destination,
            message?.let { messageSerializer.serialize(message) },
            messageSerializer.contentType()
        )

    @Suppress("UNCHECKED_CAST")
    override fun send(destination: AgentMessageDestination, message: T?): ResponseContent<R> {
        val response = send(destination, message, messageSerializer.contentType())
        return HttpResponseContent(
            response.statusObject as Int,
            messageDeserializer.deserialize((response as ResponseContent<ByteArray>).content)
        )
    }

}