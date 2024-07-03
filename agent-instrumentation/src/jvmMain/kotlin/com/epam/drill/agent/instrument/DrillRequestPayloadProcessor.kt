package com.epam.drill.agent.instrument

private const val PAYLOAD_PREFIX = "\n\ndrill-payload-begin\n"
private const val PAYLOAD_SUFFIX = "\ndrill-payload-end"

class DrillRequestPayloadProcessor(
    private val enabled: Boolean = true,
    private val headersProcessor: HeadersProcessor
) : PayloadProcessor {

    override fun retrieveDrillHeaders(message: String) = message.takeIf { it.endsWith(PAYLOAD_SUFFIX) }
        ?.removeSuffix(PAYLOAD_SUFFIX)
        ?.substringAfter(PAYLOAD_PREFIX)
        ?.split("\n")
        ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        ?.also(headersProcessor::storeHeaders)
        ?.let { message.substringBefore(PAYLOAD_PREFIX) }
        ?: message

    override fun retrieveDrillHeaders(message: ByteArray) =
        retrieveDrillHeaders(message.decodeToString()).encodeToByteArray()

    override fun storeDrillHeaders(message: String?) = message
        ?.let { headersProcessor.retrieveHeaders() }
        ?.map { (k, v) -> "$k=$v" }
        ?.joinToString("\n", PAYLOAD_PREFIX, PAYLOAD_SUFFIX)
        ?.let(message::plus)

    override fun storeDrillHeaders(message: ByteArray?) = message
        ?.let { storeDrillHeaders(message.decodeToString())!!.encodeToByteArray() }

    override fun isPayloadProcessingEnabled() = enabled

    override fun isPayloadProcessingSupported(headers: Map<String, String>) =
        headers.containsKey("drill-ws-per-message") && headers["drill-ws-per-message"].toBoolean()

}
