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

import com.epam.drill.hpack.HeaderListener
import com.epam.drill.hpack.toInputStream

data class ReadStream(
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
    override var now: Boolean = false
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

    val headers = mutableMapOf<ByteArray, ByteArray>()
    val headerListener = object : HeaderListener {
        override fun addHeader(name: ByteArray?, value: ByteArray?, sensitive: Boolean) {
            headers[name!!] = value!!
        }
    }

    override fun readPayload(): Boolean {
        frm.frameEndIndex = index
        val i = byteArray.size - index
        return if (i > 0 && i >= frm.length) {
            val readBody = readBody(frm.length)

            if (frm.type == 0x1) {
                payloadIndex = index + MAGIC_NUMBER
                decoder.decode(
                    readBody.copyOfRange(MAGIC_NUMBER, readBody.size).toInputStream(),
                    headerListener
                )
                now = true
            } else {
                fianlBytes += readBody
            }
            currentStep = 0
            true
        } else false
    }


    fun readHeaders(array: ByteArray): MutableMap<ByteArray, ByteArray>? {
        if (now) return null
        readPacket(array)
        readMainHeaders()
        while (!isClosed && doTask()) {
            if (now) {
                isClosed = true
                return headers
            }
        }
        return null
    }


}
