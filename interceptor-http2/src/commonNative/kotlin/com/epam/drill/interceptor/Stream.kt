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

import com.epam.drill.interceptor.haffman.MAX_HEADER_SIZE
import com.epam.drill.interceptor.haffman.MAX_HEADER_TABLE_SIZE
import com.epam.drill.hpack.Decoder

abstract class Stream(
    open var packetEndIndex: Int = 0,
    open var packetStartIndex: Int = 0,
    open val frm: Frame = Frame(),
    open var byteArray: ByteArray = byteArrayOf(),
    open var fianlBytes: ByteArray = byteArrayOf(),
    open var index: Int = 0,
    open var isClosed: Boolean = false,
    open var isHeaderRead: Boolean = false,
    open var currentStep: Int = 0,
    open var payloadIndex: Int = 0,
    open var now: Boolean = false
) {
    val MAGIC_NUMBER = 5

    val decoder = Decoder(
        MAX_HEADER_SIZE,
        MAX_HEADER_TABLE_SIZE
    )

    fun isOk(): Boolean = !isClosed

    fun doTask(): Boolean {
        val b = readLength()
        val b1 = readType()
        val b2 = readFlags()
        val b3 = readStreamIdentifier()
        val b4 = readPayload()
        return b || b1 || b2 || b3 || b4
    }


    fun readLength(): Boolean {
        return if (currentStep == 0 && (byteArray.size - index) > 2) {
            frm.frameStartIndex = index
            frm.length = read24BE()
            fianlBytes += frm.length.write24BE()
            currentStep++
            true
        } else false
    }

    open fun readType(): Boolean {
        return if (currentStep == 1 && (byteArray.size - index) > 0) {
            frm.type = read()
            fianlBytes += byteArrayOf(frm.type.toByte())
            currentStep++
            true
        } else false
    }


    fun readFlags(): Boolean {
        return if ((currentStep == 2) && (byteArray.size - index) > 0) {
            frm.flags = read()
            fianlBytes += byteArrayOf(frm.flags.toByte())
            currentStep++
            true
        } else false
    }

    fun readStreamIdentifier(): Boolean {
        return if (currentStep == 3 && (byteArray.size - index) > 3) {
            frm.streamIdentifier = read32BE()
            fianlBytes += frm.streamIdentifier.write32BE()
            currentStep++
            true
        } else false
    }

    abstract fun readPayload(): Boolean

    fun readPacket(array: ByteArray) {
        packetStartIndex = byteArray.size
        packetEndIndex = packetStartIndex + array.size
        byteArray += array
    }

    fun readMainHeaders() {
        if (!isHeaderRead) {
            val firsHeaderIndex = byteArray.indexOf(HEADERS_DELIMITER) + 4
            val copyOfRange = byteArray.copyOfRange(firsHeaderIndex, byteArray.size)
            index = firsHeaderIndex + copyOfRange.indexOf(HEADERS_DELIMITER) + 4
            isHeaderRead = true
            fianlBytes += byteArray.copyOfRange(0, index)
        }
    }


    fun read24BE(): Int {
        index += 3
        return byteArray.copyOfRange(index - 3, index).read24BE(0)
    }

    fun read32BE(): Int {
        index += 4
        return byteArray.copyOfRange(index - 4, index).read32BE(0)
    }

    fun read(): Int {
        index += 1
        return byteArray[index - 1].toInt()
    }

    fun readBody(final: Int): ByteArray {
        return byteArray.copyOfRange(index, index + final).apply { index += final }
    }

}