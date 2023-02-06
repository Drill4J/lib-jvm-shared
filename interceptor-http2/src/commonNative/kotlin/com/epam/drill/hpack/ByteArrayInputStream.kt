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

import kotlin.math.min

fun ByteArray.toInputStream(): ByteArrayInputStream {
    return ByteArrayInputStream(this)
}

class ByteArrayInputStream : InputStream {

    protected var buf: ByteArray

    protected var pos: Int

    protected var mark = 0


    protected var count: Int


    constructor(buf: ByteArray) {
        this.buf = buf
        pos = 0
        count = buf.size
    }

    constructor(buf: ByteArray, offset: Int, length: Int) {
        this.buf = buf
        pos = offset
        count = min(offset + length, buf.size)
        mark = offset
    }


    override fun read(): Int {
        return if (pos < count) buf[pos++].toInt() and 0xff else -1
    }


    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var len = len
        if (pos >= count) {
            return -1
        }
        val avail = count - pos
        if (len > avail) {
            len = avail
        }
        if (len <= 0) {
            return 0
        }
        System.arraycopy(buf, pos, b, off, len)
        pos += len
        return len
    }


    override fun readAllBytes(): ByteArray? {
        val result: ByteArray = buf.copyOfRange(pos, count)
        pos = count
        return result
    }

    override fun readNBytes(b: ByteArray, off: Int, len: Int): Int {
        val n = read(b, off, len)
        return if (n == -1) 0 else n
    }


    override fun transferTo(out: OutputStream): Long {
        val len = count - pos
        out.write(buf, pos, len)
        pos = count
        return len.toLong()
    }


    override fun skip(n: Long): Long {
        var k = count - pos.toLong()
        if (n < k) {
            k = if (n < 0) 0 else n
        }
        pos += k.toInt()
        return k
    }


    override fun available(): Int {
        return count - pos
    }

    override fun markSupported(): Boolean {
        return true
    }


    override fun mark(readAheadLimit: Int) {
        mark = pos
    }



    override fun reset() {
        pos = mark
    }

    override fun close() {
    }
}