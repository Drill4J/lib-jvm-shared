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
package com.epam.drill.plugin

data class DrillRequest(
    val drillSessionId: String?,
    val host: String?,
    val additionalConfig: String?,
    val headers: Map<String, String>
) {
    fun get(key: String?): String? = headers[key]
}

typealias RawHttpRequest = String

const val COOKIE_HEADER_NAME = "cookie"
const val DRILL_ADDITIONAL_CONFIG = "drill-additional-config"
const val HOST = "host"
const val DRILL_SESSION_ID = "drill-session-id"

fun parseHttpRequest(request: RawHttpRequest): HttpRequest {
    val reader = request.lineSequence().takeWhile { it.isNotBlank() }
    val query = reader.first().toRequestQuery()
    val requestHeaders = parseHeaders(reader.drop(1))
    val cookies = parseCookies(requestHeaders)
    return HttpRequest(query, requestHeaders, cookies)

}

private fun parseCookies(requestHeaders: Map<String, String>) = requestHeaders[COOKIE_HEADER_NAME]?.run {
    val cookies = mutableMapOf<String, String>()
    val split = this.trimStart().split("; ").dropLastWhile { it.isBlank() }
    for (rawCookie in split) {
        val eqIndex = rawCookie.indexOfFirst { it == '=' }
        val key = rawCookie.slice(0 until eqIndex)
        val value = rawCookie.slice(eqIndex + 1 until rawCookie.length)
        cookies[key] = value
    }
    cookies
} ?: mutableMapOf()


private fun parseHeaders(rawHeaders: Sequence<String>) = rawHeaders.filter { it.isNotBlank() }.associate { parseHeaderLine(it) }

private fun parseHeaderLine(header: String): Pair<String, String> {
    val idx = header.indexOfFirst { it == ':' }
    if (idx == -1) {
        throw RuntimeException("Invalid Header Parameter: $header")
    }
    return header.substring(0, idx).toLowerCase() to header.substring(idx + 2, header.length)
}


data class HttpRequest(
    val query: Query,
    val headers: Map<String, String>,
    val cookies: Map<String, String>
)

fun HttpRequest.toRawRequestString() =
    "$query\n" +
            "${headers.map { it.key + ": " + it.value }.joinToString("\n")}\n" +
            "$COOKIE_HEADER_NAME:  ${cookies.map { it.key + "=" + it.value }.joinToString("; ")}"

fun HttpRequest.toDrillRequest(): DrillRequest {
    val optimizedHeaders = this.headers.mapKeys { it.key.toLowerCase() }
    return DrillRequest(
        optimizedHeaders[DRILL_SESSION_ID] ?: this.cookies[DRILL_SESSION_ID],
        this.headers[HOST],
        this.headers[DRILL_ADDITIONAL_CONFIG],
        optimizedHeaders
    )
}

fun RawHttpRequest.toRequestQuery(): Query {
    val queryValues = this.split(" ")
    return Query(queryValues[0], queryValues[1], queryValues[2])
}

data class Query(val method: String, val url: String, val version: String) {
    override fun toString(): String {
        return "$method $url $version"
    }
}