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
package com.epam.drill.hpack

import kotlin.math.max


class ByteArrayOutputStream  constructor(size: Int = 32) : OutputStream() {

    protected var buf: ByteArray


    protected var count = 0


    private fun ensureCapacity(minCapacity: Int) {
        // overflow-conscious code
        val oldCapacity = buf.size
        val minGrowth = minCapacity - oldCapacity
        if (minGrowth > 0) {
            buf = buf.copyOf(newLength(oldCapacity,
                    minGrowth, oldCapacity /* preferred growth */))
        }
    }

    fun newLength(oldLength: Int, minGrowth: Int, prefGrowth: Int): Int {
        // assert oldLength >= 0
        // assert minGrowth > 0
        val newLength = max(minGrowth, prefGrowth) + oldLength
        return if (newLength - (Int.MAX_VALUE - 8) <= 0) {
            newLength
        } else hugeLength(oldLength, minGrowth)
    }

    private fun hugeLength(oldLength: Int, minGrowth: Int): Int {
        val minLength = oldLength + minGrowth
        if (minLength < 0) { // overflow
            throw OutOfMemoryError("Required array length too large")
        }
        return if (minLength <= Int.MAX_VALUE - 8) {
            Int.MAX_VALUE - 8
        } else Int.MAX_VALUE
    }


    override fun write(b: Int) {
        ensureCapacity(count + 1)
        buf[count] = b.toByte()
        count += 1
    }



    override fun write(b: ByteArray, off: Int, len: Int) {
        ensureCapacity(count + len)
        System.arraycopy(b, off, buf, count, len)
        count += len
    }


    fun writeBytes(b: ByteArray) {
        write(b, 0, b.size)
    }



    @Throws(IOException::class)
    fun writeTo(out: OutputStream) {
        out.write(buf, 0, count)
    }



    fun reset() {
        count = 0
    }



    fun toByteArray(): ByteArray {
        return buf.copyOf(count)
    }



    fun size(): Int {
        return count
    }



    override fun close() {
    }

    init {
        require(size >= 0) {
            ("Negative initial size: "
                    + size)
        }
        buf = ByteArray(size)
    }
}