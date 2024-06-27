package com.epam.drill.agent.instrument

interface PayloadProcessor {
    fun retrievePayload(message: String): Pair<String, Map<String, String>>
    fun retrievePayload(message: ByteArray): Pair<ByteArray, Map<String, String>>
    fun storePayload(message: String, headers: Map<String, String>): String
    fun storePayload(message: ByteArray, headers: Map<String, String>): ByteArray
}
