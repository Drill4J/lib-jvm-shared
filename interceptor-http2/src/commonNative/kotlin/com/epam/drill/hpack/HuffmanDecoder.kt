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


class HuffmanDecoder(codes: IntArray?, lengths: ByteArray?) {
    private val root: Node

    /**
     * Decompresses the given Huffman coded string literal.
     * @param  buf the string literal to be decoded
     * @return the output stream for the compressed data
     * @throws IOException if an I/O error occurs. In particular,
     * an `IOException` may be thrown if the
     * output stream has been closed.
     */
    @Throws(IOException::class)
    fun decode(buf: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        var node: Node? = root
        var current = 0
        var bits = 0
        for (i in buf.indices) {
            val b: Int = buf[i].toInt() and 0xFF
            current = current shl 8 or b
            bits += 8
            while (bits >= 8) {
                val c = current ushr bits - 8 and 0xFF
                node = node!!.children!![c]
                bits -= node!!.bits
                if (node.isTerminal) {
                    if (node.symbol == HpackUtil.HUFFMAN_EOS) {
                        throw EOS_DECODED
                    }
                    baos.write(node.symbol)
                    node = root
                }
            }
        }
        while (bits > 0) {
            val c = current shl 8 - bits and 0xFF
            node = node!!.children!![c]
            if (node!!.isTerminal && node.bits <= bits) {
                bits -= node.bits
                baos.write(node.symbol)
                node = root
            } else {
                break
            }
        }

        // Section 5.2. String Literal Representation
        // Padding not corresponding to the most significant bits of the code
        // for the EOS symbol (0xFF) MUST be treated as a decoding error.
        val mask = (1 shl bits) - 1
        if (current and mask != mask) {
            throw INVALID_PADDING
        }
        return baos.toByteArray()
    }

    private class Node {
        val symbol // terminal nodes have a symbol
                : Int
        val bits // number of bits matched by the node
                : Int
        val children // internal nodes have children
                : Array<Node?>?

        /**
         * Construct an internal node
         */
        constructor() {
            symbol = 0
            bits = 8
            children = arrayOfNulls(256)
        }

        /**
         * Construct a terminal node
         * @param symbol the symbol the node represents
         * @param bits   the number of bits matched by this node
         */
        constructor(symbol: Int, bits: Int) {
            assert(bits > 0 && bits <= 8)
            this.symbol = symbol
            this.bits = bits
            children = null
        }

        val isTerminal: Boolean
             get() = children == null
    }

    companion object {
        private val EOS_DECODED = IOException("EOS Decoded")
        private val INVALID_PADDING = IOException("Invalid Padding")
        private fun buildTree(codes: IntArray?, lengths: ByteArray?): Node {
            val root = Node()
            for (i in codes!!.indices) {
                insert(root, i, codes[i], lengths!![i])
            }
            return root
        }

        private fun insert(root: Node, symbol: Int, code: Int, length: Byte) {
            // traverse tree using the most significant bytes of code
            var length = length.toInt()
            var current: Node? = root
            while (length > 8) {
                check(!current!!.isTerminal) { "invalid Huffman code: prefix not unique" }
                length -= 8
                val i = code ushr length.toInt() and 0xFF
                if (current!!.children!![i] == null) {
                    current.children!![i] = Node()
                }
                current = current.children!![i]
            }
            val terminal = Node(symbol, length.toInt())
            val shift = 8 - length
            val start = code shl shift and 0xFF
            val end = 1 shl shift
            for (i in start until start + end) {
                current!!.children!![i] = terminal
            }
        }
    }

    /**
     * Creates a new Huffman decoder with the specified Huffman coding.
     * @param codes   the Huffman codes indexed by symbol
     * @param lengths the length of each Huffman code
     */
    init {
        require(!(codes!!.size != 257 || codes.size != lengths!!.size)) { "invalid Huffman coding" }
        root = buildTree(codes, lengths)
    }
}