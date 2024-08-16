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

import java.util.Objects
import mu.KLogger
import mu.KotlinLogging
import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.common.agent.request.HeadersRetriever
import com.epam.drill.common.agent.request.RequestHolder

open class DrillRequestHeadersProcessor(
    private val headersRetriever: HeadersRetriever,
    private val requestHolder: RequestHolder
) : HeadersProcessor {

    private val logger: KLogger = KotlinLogging.logger {}

    override fun removeHeaders() = requestHolder.remove()

    override fun storeHeaders(headers: Map<String, String>) = try {
        headers[headersRetriever.sessionHeader()]?.also { sessionId ->
            headers.filterKeys(Objects::nonNull)
                .filter { it.key.startsWith(HeadersProcessor.DRILL_HEADER_PREFIX) }
                .also { logger.trace { "storeHeaders: Storing headers, sessionId=$sessionId: $it" } }
                .also { requestHolder.store(DrillRequest(sessionId, it)) }
        }
        Unit
    } catch (e: Exception) {
        logger.error(e) { "storeHeaders: Error while storing headers" }
    }

    override fun retrieveHeaders() = try {
        requestHolder.retrieve()?.let { drillRequest ->
            drillRequest.headers.filter { it.key.startsWith(HeadersProcessor.DRILL_HEADER_PREFIX) }
                .plus(headersRetriever.sessionHeader() to drillRequest.drillSessionId)
                .also { logger.trace { "retrieveHeaders: Getting headers, sessionId=${drillRequest.drillSessionId}: $it" } }
        }
    } catch (e: Exception) {
        logger.error(e) { "retrieveHeaders: Error while loading drill headers" }
        null
    }

    override fun hasHeaders() = requestHolder.retrieve() != null

    override fun isProcessRequests() = true

    override fun isProcessResponses() = true

}
