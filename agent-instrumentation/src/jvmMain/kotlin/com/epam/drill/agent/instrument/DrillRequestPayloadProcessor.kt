package com.epam.drill.agent.instrument

private const val PAYLOAD_PREFIX = "\n\ndrill-payload-begin\n"
private const val PAYLOAD_SUFFIX = "\ndrill-payload-end"

class DrillRequestPayloadProcessor : PayloadProcessor {

    override fun retrievePayload(message: String) = message
        .takeIf { it.endsWith(PAYLOAD_SUFFIX) }
        ?.removeSuffix(PAYLOAD_SUFFIX)
        ?.removePrefix(PAYLOAD_PREFIX)
        ?.split("\n")
        ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        ?.let(message.substringBefore(PAYLOAD_PREFIX)::to)
        ?: emptyMap<String, String>().let(message::to)

    override fun retrievePayload(message: ByteArray) =
        retrievePayload(message.decodeToString()).let { it.first.encodeToByteArray() to it.second }

    override fun storePayload(message: String, headers: Map<String, String>) = headers
        .map { (k, v) -> "$k=$v" }
        .joinToString("\n", PAYLOAD_PREFIX, PAYLOAD_SUFFIX)
        .let(message::plus)

    override fun storePayload(message: ByteArray, headers: Map<String, String>) =
        storePayload(message.decodeToString(), headers).encodeToByteArray()

}
