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
