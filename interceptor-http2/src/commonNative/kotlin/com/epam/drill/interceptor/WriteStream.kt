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

import com.epam.drill.hpack.ByteArrayOutputStream
import com.epam.drill.hpack.Encoder

data class WriteStream(
    override var packetEndIndex: Int = 0,
    override var packetStartIndex: Int = 0,
    override val frm: Frame = Frame(),
    override var byteArray: ByteArray = byteArrayOf(),
    override var fianlBytes: ByteArray = byteArrayOf(),
    override var index: Int = 0,
    override var isClosed: Boolean = false,
    override var isHeaderRead: Boolean = false,
    override var currentStep: Int = 0,
    override var payloadIndex: Int = 0,
    override var now: Boolean = false,
    var isSizeModified: Boolean = false,
    val headers: Map<String, String> = emptyMap()
) : Stream(
    packetEndIndex,
    packetStartIndex,
    frm,
    byteArray,
    fianlBytes,
    index,
    isClosed,
    isHeaderRead,
    currentStep,
    payloadIndex,
    now
) {
    private val headersToIbject: ByteArray = run {
        val createEncoder = Encoder(Int.MAX_VALUE, true, false, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        headers.forEach { (k, v) ->
            createEncoder.encodeHeader(
                byteArrayOutputStream,
                k.encodeToByteArray(),
                v.encodeToByteArray(),
                false
            )
        }
        byteArrayOutputStream.toByteArray()
    }

    override fun readType(): Boolean {
        return if (currentStep == 1 && (byteArray.size - index) > 0) {
            frm.type = read()
            if (frm.type == 1) {
                val write24BE = (frm.length + headersToIbject.size).write24BE()
                fianlBytes[fianlBytes.size - 3] = write24BE[0]
                fianlBytes[fianlBytes.size - 2] = write24BE[1]
                fianlBytes[fianlBytes.size - 1] = write24BE[2]

                isSizeModified = true
            }
            fianlBytes += byteArrayOf(frm.type.toByte())
            currentStep++
            true
        } else false
    }

    override fun readPayload(): Boolean {
        val i = byteArray.size - index
        return if (i > 0 && i >= frm.length) {
            val readBody = readBody(frm.length)
            frm.frameEndIndex = index
            if (frm.type == 0x1) {
                payloadIndex = index + MAGIC_NUMBER

                val bytes = readBody + headersToIbject
                frm.payload = bytes
                fianlBytes += bytes
                now = true
            } else {
                fianlBytes += readBody
            }
            currentStep = 0
            true

        } else false
    }


    fun writeHeaders(array: ByteArray): ByteArray? {
        if (now) return null
        readPacket(array)
        readMainHeaders()
        while (!isClosed && doTask()) {

            if (now) {
                isClosed = true
                return (fianlBytes.copyOfRange(
                    packetStartIndex,
                    fianlBytes.size
                ) + byteArray.copyOfRange(
                    frm.frameEndIndex,
                    packetEndIndex
                ))
            } else if (isSizeModified) {
                return (fianlBytes.copyOfRange(
                    packetStartIndex,
                    fianlBytes.size
                ) + byteArray.copyOfRange(
                    fianlBytes.size,
                    packetEndIndex
                ))
            }

        }
        return null
    }

}
