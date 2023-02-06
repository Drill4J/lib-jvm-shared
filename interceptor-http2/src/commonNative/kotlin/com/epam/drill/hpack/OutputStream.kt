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


abstract class OutputStream : Closeable, Flushable {

    @Throws(IOException::class)
    abstract fun write(b: Int)

    @Throws(IOException::class)
    fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @Throws(IOException::class)
    open fun write(b: ByteArray, off: Int, len: Int) {
        // len == 0 condition implicitly handled by loop bounds
        for (i in 0 until len) {
            write(b[off + i].toInt())
        }
    }

    @Throws(IOException::class)
    override fun flush() {
    }


    override fun close() {
    }

    companion object {

        fun nullOutputStream(): OutputStream {
            return object : OutputStream() {
                private var closed = false

                @Throws(IOException::class)
                private fun ensureOpen() {
                    if (closed) {
                        throw IOException("Stream closed")
                    }
                }

                @Throws(IOException::class)
                override fun write(b: Int) {
                    ensureOpen()
                }

                @Throws(IOException::class)
                override fun write(b: ByteArray, off: Int, len: Int) {
                    ensureOpen()
                }

                override fun close() {
                    closed = true
                }
            }
        }
    }
}