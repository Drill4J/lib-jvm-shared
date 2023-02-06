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
package com.epam.drill.interceptor

import com.epam.drill.hook.gen.*
import com.epam.drill.hook.io.*
import com.epam.drill.hook.io.tcp.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

//TODO EPMDJ-8696 Move back to common module

actual fun configureHttpInterceptor() {
    configureTcpHooks()
    addInterceptor(HttpInterceptor().freeze())
}

@ThreadLocal
private var reader = mutableMapOf<DRILL_SOCKET, ByteArray?>()


class HttpInterceptor : Interceptor {
    override fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) {
        try {
            bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString().let { prefix ->
                val readBytesClb = { bytes.readBytes(size.convert()) }
                when {
                    HTTP_VERBS.any { prefix.startsWith(it) } -> {
                        readBytesClb().let { readBytes -> processHttpRequest(readBytes, fd) { readBytes } }
                    }
                    reader[fd] != null -> {
                        readBytesClb().let { readBytes ->
                            processHttpRequest(readBytes, fd) {
                                reader.remove(fd)?.plus(readBytes)
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    override fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData {
        try {
            val readBytes = bytes.readBytes(size.convert())
            if (readBytes.decodeToString().startsWith("PRI")) {
                println(readBytes.contentToString())
                return TcpFinalData(bytes, size)
            } else {
                val index = readBytes.indexOf(CR_LF_BYTES)
                if (index > 0) {
                    val httpWriteHeaders = injectedHeaders.value()
                    if (isNotContainsDrillHeaders(readBytes, httpWriteHeaders)) {
                        val firstLineOfResponse = readBytes.copyOfRange(FIRST_INDEX, index)
                        val injectedHeader = prepareHeaders(httpWriteHeaders)
                        val responseTail = readBytes.copyOfRange(index, size.convert())
                        val modified = firstLineOfResponse + injectedHeader + responseTail

                        logger.trace {
                            "App write http by '$fd' fd: \n\t${
                                (readBytes.copyOfRange(
                                    FIRST_INDEX,
                                    readBytes.indexOf(HEADERS_DELIMITER).let { if (it != -1) it else readBytes.size }
                                )).decodeToString().replace("\r\n", "\r\n\t")
                            }"
                        }
                        writeCallback.value(modified)
                        return TcpFinalData(
                            modified.toCValues().getPointer(this),
                            modified.size,
                            injectedHeader.size
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
        return TcpFinalData(bytes, size)
    }

    override fun close(fd: DRILL_SOCKET) {
        reader.remove(fd)
    }

    override fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>): Boolean {
        return HTTP_VERBS.any { bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString().startsWith(it) }
    }

}

private fun processHttpRequest(readBytes: ByteArray, fd: DRILL_SOCKET, dataCallback: (() -> ByteArray?)) =
    if (notContainsFullHeadersPart(readBytes)) {
        reader[fd] = reader[fd] ?: (byteArrayOf() + readBytes)
    } else {
        dataCallback()?.let {
            logger.trace { "App read http by '$fd' fd: \n\t${it.decodeToString().replace("\r\n", "\r\n\t")}" }
            val decodeToString = it.decodeToString()

            readHeaders.value(
                decodeToString.subSequence(
                    decodeToString.indexOfFirst { it == '\r' },
                    decodeToString.indexOf("\r\n\r\n")
                ).split("\r\n").filter { it.isNotBlank() }.associate {
                    val (k, v) = it.split(":", limit = 2)
                    k.trim().encodeToByteArray() to v.trim().encodeToByteArray()
                })
            readCallback.value(it)
        }

    }


private fun notContainsFullHeadersPart(readBytes: ByteArray) = readBytes.indexOf(HEADERS_DELIMITER) == -1


private fun prepareHeaders(httpWriteHeaders: Map<String, String>) =
    CR_LF_BYTES + httpWriteHeaders.map { (k, v) -> "$k: $v" }.joinToString(CR_LF).encodeToByteArray()

private fun isNotContainsDrillHeaders(readBytes: ByteArray, httpWriteHeaders: Map<String, String>) =
    httpWriteHeaders.isNotEmpty() && readBytes.indexOf(httpWriteHeaders.entries.first().key.encodeToByteArray()) == -1
