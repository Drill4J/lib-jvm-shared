package com.epam.drill.agent.instrument

interface PayloadProcessor {
    fun retrieveDrillHeaders(message: String): String
    fun retrieveDrillHeaders(message: ByteArray): ByteArray
    fun storeDrillHeaders(message: String): String
    fun storeDrillHeaders(message: ByteArray): ByteArray
    fun isPayloadProcessingEnabled(): Boolean
    fun isPayloadProcessingSupported(headers: Map<String, String>): Boolean
}
