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

import com.epam.drill.hpack.HpackUtil.IndexType

class Encoder(
        maxHeaderTableSize: Int,
        useIndexing: Boolean,
        forceHuffmanOn: Boolean,
        forceHuffmanOff: Boolean
) {
    // for testing
    private val useIndexing: Boolean
    private val forceHuffmanOn: Boolean
    private val forceHuffmanOff: Boolean

    // a linked hash map of header fields
    private val headerFields = arrayOfNulls<HeaderEntry>(BUCKET_SIZE)
    private val head = HeaderEntry(-1, EMPTY, EMPTY, Int.MAX_VALUE, null)
    private var size = 0


    var maxHeaderTableSize: Int
        private set

    /**
     * Creates a new encoder.
     */
    constructor(maxHeaderTableSize: Int) : this(maxHeaderTableSize, true, false, false) {}

    /**
     * Encode the header field into the header block.
     */
    @Throws(IOException::class)
    fun encodeHeader(out: OutputStream, name: ByteArray, value: ByteArray, sensitive: Boolean) {

        // If the header value is sensitive then it must never be indexed
        if (sensitive) {
            val nameIndex = getNameIndex(name)
            encodeLiteral(out, name, value, IndexType.NEVER, nameIndex)
            return
        }

        // If the peer will only use the static table
        if (maxHeaderTableSize == 0) {
            val staticTableIndex = StaticTable.getIndex(name, value)
            if (staticTableIndex == -1) {
                val nameIndex = StaticTable.getIndex(name)
                encodeLiteral(out, name, value, IndexType.NONE, nameIndex)
            } else {
                encodeInteger(out, 0x80, 7, staticTableIndex)
            }
            return
        }
        val headerSize: Int = HeaderField.Companion.sizeOf(name, value)

        // If the headerSize is greater than the max table size then it must be encoded literally
        if (headerSize > maxHeaderTableSize) {
            val nameIndex = getNameIndex(name)
            encodeLiteral(out, name, value, IndexType.NONE, nameIndex)
            return
        }
        val headerField = getEntry(name, value)
        if (headerField != null) {
            val index = getIndex(headerField.index) + StaticTable.length
            // Section 6.1. Indexed Header Field Representation
            encodeInteger(out, 0x80, 7, index)
        } else {
            val staticTableIndex = StaticTable.getIndex(name, value)
            if (staticTableIndex != -1) {
                // Section 6.1. Indexed Header Field Representation
                encodeInteger(out, 0x80, 7, staticTableIndex)
            } else {
                val nameIndex = getNameIndex(name)
                if (useIndexing) {
                    ensureCapacity(headerSize)
                }
                val indexType = if (useIndexing) IndexType.INCREMENTAL else IndexType.NONE
                encodeLiteral(out, name, value, indexType, nameIndex)
                if (useIndexing) {
                    add(name, value)
                }
            }
        }
    }

    /**
     * Set the maximum table size.
     */
    @Throws(IOException::class)
    fun setMaxHeaderTableSize(out: OutputStream, maxHeaderTableSize: Int) {
        require(maxHeaderTableSize >= 0) { "Illegal Capacity: $maxHeaderTableSize" }
        if (this.maxHeaderTableSize == maxHeaderTableSize) {
            return
        }
        this.maxHeaderTableSize = maxHeaderTableSize
        ensureCapacity(0)
        encodeInteger(out, 0x20, 5, maxHeaderTableSize)
    }

    /**
     * Encode string literal according to Section 5.2.
     */
    @Throws(IOException::class)
    private fun encodeStringLiteral(out: OutputStream, string: ByteArray) {
        val huffmanLength = Huffman.ENCODER.getEncodedLength(string)
        if (huffmanLength < string.size && !forceHuffmanOff || forceHuffmanOn) {
            encodeInteger(out, 0x80, 7, huffmanLength)
            Huffman.ENCODER.encode(out, string)
        } else {
            encodeInteger(out, 0x00, 7, string.size)
            out.write(string, 0, string.size)
        }
    }

    /**
     * Encode literal header field according to Section 6.2.
     */
    @Throws(IOException::class)
    private fun encodeLiteral(out: OutputStream, name: ByteArray, value: ByteArray, indexType: IndexType, nameIndex: Int) {
        val mask: Int
        val prefixBits: Int
        when (indexType) {
            IndexType.INCREMENTAL -> {
                mask = 0x40
                prefixBits = 6
            }
            IndexType.NONE -> {
                mask = 0x00
                prefixBits = 4
            }
            IndexType.NEVER -> {
                mask = 0x10
                prefixBits = 4
            }
            else -> throw IllegalStateException("should not reach here")
        }
        encodeInteger(out, mask, prefixBits, if (nameIndex == -1) 0 else nameIndex)
        if (nameIndex == -1) {
            encodeStringLiteral(out, name)
        }
        encodeStringLiteral(out, value)
    }

    private fun getNameIndex(name: ByteArray): Int {
        var index = StaticTable.getIndex(name)
        if (index == -1) {
            index = getIndex(name)
            if (index >= 0) {
                index += StaticTable.length
            }
        }
        return index
    }

    /**
     * Ensure that the dynamic table has enough room to hold 'headerSize' more bytes.
     * Removes the oldest entry from the dynamic table until sufficient space is available.
     */
    @Throws(IOException::class)
    private fun ensureCapacity(headerSize: Int) {
        while (size + headerSize > maxHeaderTableSize) {
            val index = length()
            if (index == 0) {
                break
            }
            remove()
        }
    }

    /**
     * Return the number of header fields in the dynamic table.
     * Exposed for testing.
     */
    fun length(): Int {
        return if (size == 0) 0 else head.after!!.index - head.before!!.index + 1
    }

    /**
     * Return the size of the dynamic table.
     * Exposed for testing.
     */
    fun size(): Int {
        return size
    }

    /**
     * Return the header field at the given index.
     * Exposed for testing.
     */
    fun getHeaderField(index: Int): HeaderField? {
        var index = index
        var entry: HeaderEntry? = head
        while (index-- >= 0) {
            entry = entry!!.before
        }
        return entry
    }

    /**
     * Returns the header entry with the lowest index value for the header field.
     * Returns null if header field is not in the dynamic table.
     */
    private fun getEntry(name: ByteArray?, value: ByteArray?): HeaderEntry? {
        if (length() == 0 || name == null || value == null) {
            return null
        }
        val h = hash(name)
        val i = index(h)
        var e = headerFields[i]
        while (e != null) {
            if (e.hash == h &&
                    HpackUtil.equals(name, e.name) &&
                    HpackUtil.equals(value, e.value)) {
                return e
            }
            e = e.next
        }
        return null
    }

    /**
     * Returns the lowest index value for the header field name in the dynamic table.
     * Returns -1 if the header field name is not in the dynamic table.
     */
    private fun getIndex(name: ByteArray?): Int {
        if (length() == 0 || name == null) {
            return -1
        }
        val h = hash(name)
        val i = index(h)
        var index = -1
        var e = headerFields[i]
        while (e != null) {
            if (e.hash == h && HpackUtil.equals(name, e.name)) {
                index = e.index
                break
            }
            e = e.next
        }
        return getIndex(index)
    }

    /**
     * Compute the index into the dynamic table given the index in the header entry.
     */
    private fun getIndex(index: Int): Int {
        return if (index == -1) {
            index
        } else index - head.before!!.index + 1
    }

    /**
     * Add the header field to the dynamic table.
     * Entries are evicted from the dynamic table until the size of the table
     * and the new header field is less than the table's capacity.
     * If the size of the new entry is larger than the table's capacity,
     * the dynamic table will be cleared.
     */
    private fun add(name: ByteArray, value: ByteArray) {
        var name = name
        var value = value
        val headerSize: Int = HeaderField.Companion.sizeOf(name, value)

        // Clear the table if the header field size is larger than the capacity.
        if (headerSize > maxHeaderTableSize) {
            clear()
            return
        }

        // Evict oldest entries until we have enough capacity.
        while (size + headerSize > maxHeaderTableSize) {
            remove()
        }

        // Copy name and value that modifications of original do not affect the dynamic table.

        name = name.copyOf(name.size)
        value = value.copyOf(value.size)
        val h = hash(name)
        val i = index(h)
        val old = headerFields[i]
        val e = HeaderEntry(h, name, value, head.before!!.index - 1, old)
        headerFields[i] = e
        e.addBefore(head)
        size += headerSize
    }

    /**
     * Remove and return the oldest header field from the dynamic table.
     */
    private fun remove(): HeaderField? {
        if (size == 0) {
            return null
        }
        val eldest = head.after
        val h = eldest!!.hash
        val i = index(h)
        var prev = headerFields[i]
        var e = prev
        while (e != null) {
            val next = e.next
            if (e === eldest) {
                if (prev === eldest) {
                    headerFields[i] = next
                } else {
                    prev!!.next = next
                }
                eldest.remove()
                size -= eldest.size()
                return eldest
            }
            prev = e
            e = next
        }
        return null
    }

    /**
     * Remove all entries from the dynamic table.
     */
    private fun clear() {
        headerFields.fill(null)
        head.after = head
        head.before = head.after
        size = 0
    }

    /**
     * A linked hash map HeaderField entry.
     */
    private class HeaderEntry
    /**
     * Creates new entry.
     */ internal constructor(var hash: Int, name: ByteArray?, value: ByteArray?, // This is used to compute the index in the dynamic table.
                             var index: Int, // These fields comprise the chained list for header fields with the same hash.
                             var next: HeaderEntry?) : HeaderField(name, value) {
        // These fields comprise the doubly linked list used for iteration.
        var before: HeaderEntry? = null
        var after: HeaderEntry? = null

        /**
         * Removes this entry from the linked list.
         */
        fun remove() {
            before!!.after = after
            after!!.before = before
            before = null // null reference to prevent nepotism with generational GC.
            after = null // null reference to prevent nepotism with generational GC.
            next = null // null reference to prevent nepotism with generational GC.
        }

        /**
         * Inserts this entry before the specified existing entry in the list.
         */
        fun addBefore(existingEntry: HeaderEntry) {
            after = existingEntry
            before = existingEntry.before
            before!!.after = this
            after!!.before = this
        }

    }

    companion object {
        private const val BUCKET_SIZE = 17
        private val EMPTY = byteArrayOf()

        /**
         * Encode integer according to Section 5.1.
         */
        @Throws(IOException::class)
        private fun encodeInteger(out: OutputStream, mask: Int, n: Int, i: Int) {
            require(!(n < 0 || n > 8)) { "N: $n" }
            val nbits = 0xFF ushr 8 - n
            if (i < nbits) {
                out.write(mask or i)
            } else {
                out.write(mask or nbits)
                var length = i - nbits
                while (true) {
                    length = if (length and 0x7F.inv() == 0) {
                        out.write(length)
                        return
                    } else {
                        out.write(length and 0x7F or 0x80)
                        length ushr 7
                    }
                }
            }
        }

        /**
         * Returns the hash code for the given header field name.
         */
        private fun hash(name: ByteArray): Int {
            var h = 0
            for (i in name.indices) {
                h = 31 * h + name[i]
            }
            return if (h > 0) {
                h
            } else if (h == Int.MIN_VALUE) {
                Int.MAX_VALUE
            } else {
                -h
            }
        }

        /**
         * Returns the index into the hash table for the hash code h.
         */
        private fun index(h: Int): Int {
            return h % BUCKET_SIZE
        }
    }

    /**
     * Constructor for testing only.
     */
    init {
        require(maxHeaderTableSize >= 0) { "Illegal Capacity: $maxHeaderTableSize" }
        this.useIndexing = useIndexing
        this.forceHuffmanOn = forceHuffmanOn
        this.forceHuffmanOff = forceHuffmanOff
        this.maxHeaderTableSize = maxHeaderTableSize
        head.after = head
        head.before = head.after
    }
}