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

abstract class InputStream : Closeable {

    abstract fun read(): Int

    fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    open fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) {
            return 0
        }
        var c = read()
        if (c == -1) {
            return -1
        }
        b[off] = c.toByte()
        var i = 1
        try {
            while (i < len) {
                c = read()
                if (c == -1) {
                    break
                }
                b[off + i] = c.toByte()
                i++
            }
        } catch (ee: IOException) {
        }
        return i
    }

    open fun readAllBytes(): ByteArray? {
        return readNBytes(Int.MAX_VALUE)
    }

    open fun readNBytes(len: Int): ByteArray? {
        require(len >= 0) { "len < 0" }
        var bufs: MutableList<ByteArray>? = null
        var result: ByteArray? = null
        var total = 0
        var remaining = len
        var n: Int
        do {
            val buf = ByteArray(min(remaining, DEFAULT_BUFFER_SIZE))
            var nread = 0

            // read to EOF which may read more or less than buffer size
            while (read(buf, nread,
                            min(buf.size - nread, remaining)).also { n = it } > 0) {
                nread += n
                remaining -= n
            }
            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw OutOfMemoryError("Required array size too large")
                }
                total += nread
                if (result == null) {
                    result = buf
                } else {
                    if (bufs == null) {
                        bufs = ArrayList()
                        bufs.add(result)
                    }
                    bufs.add(buf)
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0)
        if (bufs == null) {
            if (result == null) {
                return ByteArray(0)
            }
            return if (result.size == total) result else result.copyOf(total)
        }
        result = ByteArray(total)
        var offset = 0
        remaining = total
        for (b in bufs) {
            val count = min(b.size, remaining)
            System.arraycopy(b, 0, result, offset, count)
            offset += count
            remaining -= count
        }
        return result
    }


    open fun readNBytes(b: ByteArray, off: Int, len: Int): Int {
        var n = 0
        while (n < len) {
            val count = read(b, off + n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }


    open fun skip(n: Long): Long {
        var remaining = n
        var nr: Int
        if (n <= 0) {
            return 0
        }
        val size = min(MAX_SKIP_BUFFER_SIZE.toLong(), remaining).toInt()
        val skipBuffer = ByteArray(size)
        while (remaining > 0) {
            nr = read(skipBuffer, 0, min(size.toLong(), remaining).toInt())
            if (nr < 0) {
                break
            }
            remaining -= nr.toLong()
        }
        return n - remaining
    }


    open fun skipNBytes(n: Long) {
        var n = n
        if (n > 0) {
            val ns = skip(n)
            if (ns >= 0 && ns < n) { // skipped too few bytes
                // adjust number to skip
                n -= ns
                // read until requested number skipped or EOS reached
                while (n > 0 && read() != -1) {
                    n--
                }
                // if not enough skipped, then EOFE
                if (n != 0L) {
                    throw EOFException()
                }
            } else if (ns != n) { // skipped negative or too many bytes
                throw IOException("Unable to skip exactly")
            }
        }
    }


    open fun available(): Int {
        return 0
    }


    override fun close() {
    }



    open fun mark(readlimit: Int) {
    }


    open fun reset() {
        throw IOException("mark/reset not supported")
    }

    open fun markSupported(): Boolean {
        return false
    }

    open fun transferTo(out: OutputStream): Long {
        var transferred: Long = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        while (this.read(buffer, 0, DEFAULT_BUFFER_SIZE).also { read = it } >= 0) {
            out.write(buffer, 0, read)
            transferred += read.toLong()
        }
        return transferred
    }

    companion object {
        // MAX_SKIP_BUFFER_SIZE is used to determine the maximum buffer size to
        // use when skipping.
        private const val MAX_SKIP_BUFFER_SIZE = 2048
        private const val DEFAULT_BUFFER_SIZE = 8192

        fun nullInputStream(): InputStream {
            return object : InputStream() {
                private var closed = false

                @Throws(IOException::class)
                private fun ensureOpen() {
                    if (closed) {
                        throw IOException("Stream closed")
                    }
                }

                override fun available(): Int {
                    ensureOpen()
                    return 0
                }

                override fun read(): Int {
                    ensureOpen()
                    return -1
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (len == 0) {
                        return 0
                    }
                    ensureOpen()
                    return -1
                }

                override fun readAllBytes(): ByteArray? {
                    ensureOpen()
                    return ByteArray(0)
                }

                override fun readNBytes(b: ByteArray, off: Int, len: Int): Int {
                    ensureOpen()
                    return 0
                }

                override fun readNBytes(len: Int): ByteArray? {
                    require(len >= 0) { "len < 0" }
                    ensureOpen()
                    return ByteArray(0)
                }

                override fun skip(n: Long): Long {
                    ensureOpen()
                    return 0L
                }

                override fun skipNBytes(n: Long) {
                    ensureOpen()
                    if (n > 0) {
                        throw EOFException()
                    }
                }

                override fun transferTo(out: OutputStream): Long {
                    ensureOpen()
                    return 0L
                }

                override fun close() {
                    closed = true
                }
            }
        }


        private const val MAX_BUFFER_SIZE = Int.MAX_VALUE - 8
    }
}