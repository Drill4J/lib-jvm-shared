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


class HuffmanEncoder
/**
 * Creates a new Huffman encoder with the specified Huffman coding.
 * @param codes   the Huffman codes indexed by symbol
 * @param lengths the length of each Huffman code
 */(private val codes: IntArray?, private val lengths: ByteArray?) {
    /**
     * Compresses the input string literal using the Huffman coding.
     * @param  out  the output stream for the compressed data
     * @param  data the string literal to be Huffman encoded
     * @param  off  the start offset in the data
     * @param  len  the number of bytes to encode
     * @throws IOException if an I/O error occurs. In particular,
     * an `IOException` may be thrown if the
     * output stream has been closed.
     */
    /**
     * Compresses the input string literal using the Huffman coding.
     * @param  out  the output stream for the compressed data
     * @param  data the string literal to be Huffman encoded
     * @throws IOException if an I/O error occurs.
     * @see com.epam.drill.hpack.HuffmanEncoder.encode
     */

    @Throws(IOException::class)
    fun encode(out: OutputStream?, data: ByteArray?, off: Int = 0, len: Int = data!!.size) {
        if (out == null) {
            throw NullPointerException("out")
        } else if (data == null) {
            throw NullPointerException("data")
        } else if (off < 0 || len < 0 || off + len < 0 || off > data.size || off + len > data.size) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return
        }
        var current: Long = 0
        var n = 0
        for (i in 0 until len) {
            val b: Int = data[off + i].toInt() and 0xFF
            val code = codes!![b]
            val nbits = lengths!![b].toInt()
            current = current shl nbits
            current = current or code.toLong()
            n += nbits
            while (n >= 8) {
                n -= 8
                out.write((current shr n).toInt())
            }
        }
        if (n > 0) {
            current = current shl 8 - n
            current = current or (0xFF ushr n).toLong() // this should be EOS symbol
            out.write(current.toInt())
        }
    }

    /**
     * Returns the number of bytes required to Huffman encode the input string literal.
     * @param  data the string literal to be Huffman encoded
     * @return the number of bytes required to Huffman encode `data`
     */
    fun getEncodedLength(data: ByteArray?): Int {
        if (data == null) {
            throw NullPointerException("data")
        }
        var len: Long = 0
        for (b in data) {
            len += lengths!![b.toInt() and 0xFF]
        }
        return (len + 7 shr 3).toInt()
    }

}