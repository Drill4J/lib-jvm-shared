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

class Decoder(maxHeaderSize: Int, maxHeaderTableSize: Int) {
    private val dynamicTable: DynamicTable
    private val maxHeaderSize: Int
    private var maxDynamicTableSize: Int
    private var encoderMaxDynamicTableSize: Int
    private var maxDynamicTableSizeChangeRequired: Boolean
    private var headerSize: Long = 0
    private var state: State? = null
    private var indexType: IndexType? = null
    private var index = 0
    private var huffmanEncoded = false
    private var skipLength = 0
    private var nameLength = 0
    private var valueLength = 0
    private var name: ByteArray? = null

    private enum class State {
        READ_HEADER_REPRESENTATION, READ_MAX_DYNAMIC_TABLE_SIZE, READ_INDEXED_HEADER, READ_INDEXED_HEADER_NAME, READ_LITERAL_HEADER_NAME_LENGTH_PREFIX, READ_LITERAL_HEADER_NAME_LENGTH, READ_LITERAL_HEADER_NAME, SKIP_LITERAL_HEADER_NAME, READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX, READ_LITERAL_HEADER_VALUE_LENGTH, READ_LITERAL_HEADER_VALUE, SKIP_LITERAL_HEADER_VALUE
    }

    private fun reset() {
        headerSize = 0
        state = State.READ_HEADER_REPRESENTATION
        indexType = IndexType.NONE
    }

    /**
     * Decode the header block into header fields.
     */
    @Throws(IOException::class)
    fun decode(`in`: InputStream, headerListener: HeaderListener) {
        loop@ while (`in`.available() > 0) {
            when (state) {
                State.READ_HEADER_REPRESENTATION -> {
                    val b = `in`.read().toByte()
                    if (maxDynamicTableSizeChangeRequired && b.toInt() and 0xE0 != 0x20) {
                        // Encoder MUST signal maximum dynamic table size change
                        throw MAX_DYNAMIC_TABLE_SIZE_CHANGE_REQUIRED
                    }
                    if (b < 0) {
                        // Indexed Header Field
                        index = b.toInt() and 0x7F
                        if (index == 0) {
                            throw ILLEGAL_INDEX_VALUE
                        } else if (index == 0x7F) {
                            state = State.READ_INDEXED_HEADER
                        } else {
                            indexHeader(index, headerListener)
                        }
                    } else if (b.toInt() and 0x40 == 0x40) {
                        // Literal Header Field with Incremental Indexing
                        indexType = IndexType.INCREMENTAL
                        index = b.toInt() and 0x3F
                        state = if (index == 0) {
                            State.READ_LITERAL_HEADER_NAME_LENGTH_PREFIX
                        } else if (index == 0x3F) {
                            State.READ_INDEXED_HEADER_NAME
                        } else {
                            // Index was stored as the prefix
                            readName(index)
                            State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX
                        }
                    } else if (b.toInt() and 0x20 == 0x20) {
                        // Dynamic Table Size Update
                        index = b.toInt() and 0x1F
                        state = if (index == 0x1F) {
                            State.READ_MAX_DYNAMIC_TABLE_SIZE
                        } else {
                            setDynamicTableSize(index)
                            State.READ_HEADER_REPRESENTATION
                        }
                    } else {
                        // Literal Header Field without Indexing / never Indexed
                        indexType = if (b.toInt() and 0x10 == 0x10) IndexType.NEVER else IndexType.NONE
                        index = b.toInt() and 0x0F
                        state = if (index == 0) {
                            State.READ_LITERAL_HEADER_NAME_LENGTH_PREFIX
                        } else if (index == 0x0F) {
                            State.READ_INDEXED_HEADER_NAME
                        } else {
                            // Index was stored as the prefix
                            readName(index)
                            State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX
                        }
                    }
                }
                State.READ_MAX_DYNAMIC_TABLE_SIZE -> {
                    val maxSize = decodeULE128(`in`)
                    if (maxSize == -1) {
                        return
                    }

                    // Check for numerical overflow
                    if (maxSize > Int.MAX_VALUE - index) {
                        throw DECOMPRESSION_EXCEPTION
                    }
                    setDynamicTableSize(index + maxSize)
                    state = State.READ_HEADER_REPRESENTATION
                }
                State.READ_INDEXED_HEADER -> {
                    val headerIndex = decodeULE128(`in`)
                    if (headerIndex == -1) {
                        return
                    }

                    // Check for numerical overflow
                    if (headerIndex > Int.MAX_VALUE - index) {
                        throw DECOMPRESSION_EXCEPTION
                    }
                    indexHeader(index + headerIndex, headerListener)
                    state = State.READ_HEADER_REPRESENTATION
                }
                State.READ_INDEXED_HEADER_NAME -> {
                    // Header Name matches an entry in the Header Table
                    val nameIndex = decodeULE128(`in`)
                    if (nameIndex == -1) {
                        return
                    }

                    // Check for numerical overflow
                    if (nameIndex > Int.MAX_VALUE - index) {
                        throw DECOMPRESSION_EXCEPTION
                    }
                    readName(index + nameIndex)
                    state = State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX
                }
                State.READ_LITERAL_HEADER_NAME_LENGTH_PREFIX -> {
                    val b = `in`.read().toByte()
                    huffmanEncoded = b.toInt() and 0x80 == 0x80
                    index = b.toInt() and 0x7F
                    if (index == 0x7f) {
                        state = State.READ_LITERAL_HEADER_NAME_LENGTH
                    } else {
                        nameLength = index

                        // Disallow empty names -- they cannot be represented in HTTP/1.x
                        if (nameLength == 0) {
                            throw DECOMPRESSION_EXCEPTION
                        }

                        // Check name length against max header size
                        if (exceedsMaxHeaderSize(nameLength.toLong())) {
                            if (indexType == IndexType.NONE) {
                                // Name is unused so skip bytes
                                name = EMPTY
                                skipLength = nameLength
                                state = State.SKIP_LITERAL_HEADER_NAME
                                break@loop
                            }

                            // Check name length against max dynamic table size
                            if (nameLength + HeaderField.Companion.HEADER_ENTRY_OVERHEAD > dynamicTable.capacity()) {
                                dynamicTable.clear()
                                name = EMPTY
                                skipLength = nameLength
                                state = State.SKIP_LITERAL_HEADER_NAME
                                break@loop
                            }
                        }
                        state = State.READ_LITERAL_HEADER_NAME
                    }
                }
                State.READ_LITERAL_HEADER_NAME_LENGTH -> {
                    // Header Name is a Literal String
                    nameLength = decodeULE128(`in`)
                    if (nameLength == -1) {
                        return
                    }

                    // Check for numerical overflow
                    if (nameLength > Int.MAX_VALUE - index) {
                        throw DECOMPRESSION_EXCEPTION
                    }
                    nameLength += index

                    // Check name length against max header size
                    if (exceedsMaxHeaderSize(nameLength.toLong())) {
                        if (indexType == IndexType.NONE) {
                            // Name is unused so skip bytes
                            name = EMPTY
                            skipLength = nameLength
                            state = State.SKIP_LITERAL_HEADER_NAME
                            break@loop
                        }

                        // Check name length against max dynamic table size
                        if (nameLength + HeaderField.Companion.HEADER_ENTRY_OVERHEAD > dynamicTable.capacity()) {
                            dynamicTable.clear()
                            name = EMPTY
                            skipLength = nameLength
                            state = State.SKIP_LITERAL_HEADER_NAME
                            break@loop
                        }
                    }
                    state = State.READ_LITERAL_HEADER_NAME
                }
                State.READ_LITERAL_HEADER_NAME -> {
                    // Wait until entire name is readable
                    if (`in`.available() < nameLength) {
                        return
                    }
                    name = readStringLiteral(`in`, nameLength)
                    state = State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX
                }
                State.SKIP_LITERAL_HEADER_NAME -> {
                    skipLength -= `in`.skip(skipLength.toLong()).toInt()
                    if (skipLength == 0) {
                        state = State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX
                    }
                }
                State.READ_LITERAL_HEADER_VALUE_LENGTH_PREFIX -> {
                    val b = `in`.read().toByte()
                    huffmanEncoded = b.toInt() and 0x80 == 0x80
                    index = b.toInt() and 0x7F
                    if (index == 0x7f) {
                        state = State.READ_LITERAL_HEADER_VALUE_LENGTH
                    } else {
                        valueLength = index

                        // Check new header size against max header size
                        val newHeaderSize = nameLength.toLong() + valueLength.toLong()
                        if (exceedsMaxHeaderSize(newHeaderSize)) {
                            // truncation will be reported during endHeaderBlock
                            headerSize = maxHeaderSize + 1.toLong()
                            if (indexType == IndexType.NONE) {
                                // Value is unused so skip bytes
                                state = State.SKIP_LITERAL_HEADER_VALUE
                                break@loop
                            }

                            // Check new header size against max dynamic table size
                            if (newHeaderSize + HeaderField.Companion.HEADER_ENTRY_OVERHEAD > dynamicTable.capacity()) {
                                dynamicTable.clear()
                                state = State.SKIP_LITERAL_HEADER_VALUE
                                break@loop
                            }
                        }
                        state = if (valueLength == 0) {
                            insertHeader(headerListener, name, EMPTY, indexType)
                            State.READ_HEADER_REPRESENTATION
                        } else {
                            State.READ_LITERAL_HEADER_VALUE
                        }
                    }
                }
                State.READ_LITERAL_HEADER_VALUE_LENGTH -> {
                    // Header Value is a Literal String
                    valueLength = decodeULE128(`in`)
                    if (valueLength == -1) {
                        return
                    }

                    // Check for numerical overflow
                    if (valueLength > Int.MAX_VALUE - index) {
                        throw DECOMPRESSION_EXCEPTION
                    }
                    valueLength += index

                    // Check new header size against max header size
                    val newHeaderSize = nameLength.toLong() + valueLength.toLong()
                    if (newHeaderSize + headerSize > maxHeaderSize) {
                        // truncation will be reported during endHeaderBlock
                        headerSize = maxHeaderSize + 1.toLong()
                        if (indexType == IndexType.NONE) {
                            // Value is unused so skip bytes
                            state = State.SKIP_LITERAL_HEADER_VALUE
                            break@loop
                        }

                        // Check new header size against max dynamic table size
                        if (newHeaderSize + HeaderField.Companion.HEADER_ENTRY_OVERHEAD > dynamicTable.capacity()) {
                            dynamicTable.clear()
                            state = State.SKIP_LITERAL_HEADER_VALUE
                            break@loop
                        }
                    }
                    state = State.READ_LITERAL_HEADER_VALUE
                }
                State.READ_LITERAL_HEADER_VALUE -> {
                    // Wait until entire value is readable
                    if (`in`.available() < valueLength) {
                        return
                    }
                    val value = readStringLiteral(`in`, valueLength)
                    insertHeader(headerListener, name, value, indexType)
                    state = State.READ_HEADER_REPRESENTATION
                }
                State.SKIP_LITERAL_HEADER_VALUE -> {
                    valueLength -= `in`.skip(valueLength.toLong()).toInt()
                    if (valueLength == 0) {
                        state = State.READ_HEADER_REPRESENTATION
                    }
                }
                else -> throw IllegalStateException("should not reach here")
            }
        }
    }

    /**
     * End the current header block. Returns if the header field has been truncated.
     * This must be called after the header block has been completely decoded.
     */
    fun endHeaderBlock(): Boolean {
        val truncated = headerSize > maxHeaderSize
        reset()
        return truncated
    }

    /**
     * Return the maximum table size.
     * This is the maximum size allowed by both the encoder and the decoder.
     */// decoder requires less space than encoder
    // encoder MUST signal this change
    /**
     * Set the maximum table size.
     * If this is below the maximum size of the dynamic table used by the encoder,
     * the beginning of the next header block MUST signal this change.
     */
    var maxHeaderTableSize: Int
        get() = dynamicTable.capacity()
        set(maxHeaderTableSize) {
            maxDynamicTableSize = maxHeaderTableSize
            if (maxDynamicTableSize < encoderMaxDynamicTableSize) {
                // decoder requires less space than encoder
                // encoder MUST signal this change
                maxDynamicTableSizeChangeRequired = true
                dynamicTable.setCapacity(maxDynamicTableSize)
            }
        }

    /**
     * Return the number of header fields in the dynamic table.
     * Exposed for testing.
     */
    fun length(): Int {
        return dynamicTable.length()
    }

    /**
     * Return the size of the dynamic table.
     * Exposed for testing.
     */
    fun size(): Int {
        return dynamicTable.size()
    }

    /**
     * Return the header field at the given index.
     * Exposed for testing.
     */
    fun getHeaderField(index: Int): HeaderField? {
        return dynamicTable.getEntry(index + 1)
    }

    @Throws(IOException::class)
    private fun setDynamicTableSize(dynamicTableSize: Int) {
        if (dynamicTableSize > maxDynamicTableSize) {
            throw INVALID_MAX_DYNAMIC_TABLE_SIZE
        }
        encoderMaxDynamicTableSize = dynamicTableSize
        maxDynamicTableSizeChangeRequired = false
        dynamicTable.setCapacity(dynamicTableSize)
    }

    @Throws(IOException::class)
    private fun readName(index: Int) {
        name = if (index <= StaticTable.length) {
            val headerField = StaticTable.getEntry(index)
            headerField!!.name
        } else if (index - StaticTable.length <= dynamicTable.length()) {
            val headerField = dynamicTable.getEntry(index - StaticTable.length)
            headerField!!.name
        } else {
            throw ILLEGAL_INDEX_VALUE
        }
    }

    @Throws(IOException::class)
    private fun indexHeader(index: Int, headerListener: HeaderListener) {
        if (index <= StaticTable.length) {
            val headerField = StaticTable.getEntry(index)
            addHeader(headerListener, headerField!!.name, headerField.value, false)
        } else if (index - StaticTable.length <= dynamicTable.length()) {
            val headerField = dynamicTable.getEntry(index - StaticTable.length)
            addHeader(headerListener, headerField!!.name, headerField.value, false)
        } else {
            throw ILLEGAL_INDEX_VALUE
        }
    }

    private fun insertHeader(headerListener: HeaderListener, name: ByteArray?, value: ByteArray?, indexType: IndexType?) {
        addHeader(headerListener, name, value, indexType == IndexType.NEVER)
        when (indexType) {
            IndexType.NONE, IndexType.NEVER -> {
            }
            IndexType.INCREMENTAL -> dynamicTable.add(HeaderField(name, value))
            else -> throw IllegalStateException("should not reach here")
        }
    }

    private fun addHeader(headerListener: HeaderListener, name: ByteArray?, value: ByteArray?, sensitive: Boolean) {
        if (name!!.size == 0) {
            throw AssertionError("name is empty")
        }
        val newSize = headerSize + name.size + value!!.size
        headerSize = if (newSize <= maxHeaderSize) {
            headerListener.addHeader(name, value, sensitive)
            newSize
        } else {
            // truncation will be reported during endHeaderBlock
            maxHeaderSize + 1.toLong()
        }
    }

    private fun exceedsMaxHeaderSize(size: Long): Boolean {
        // Check new header size against max header size
        if (size + headerSize <= maxHeaderSize) {
            return false
        }

        // truncation will be reported during endHeaderBlock
        headerSize = maxHeaderSize + 1.toLong()
        return true
    }

    @Throws(IOException::class)
    private fun readStringLiteral(`in`: InputStream, length: Int): ByteArray? {
        val buf = ByteArray(length)
        if (`in`.read(buf) != length) {
            throw DECOMPRESSION_EXCEPTION
        }
        return if (huffmanEncoded) {
            Huffman.DECODER.decode(buf)
        } else {
            buf
        }
    }

    companion object {
        private val DECOMPRESSION_EXCEPTION = IOException("decompression failure")
        private val ILLEGAL_INDEX_VALUE = IOException("illegal index value")
        private val INVALID_MAX_DYNAMIC_TABLE_SIZE = IOException("invalid max dynamic table size")
        private val MAX_DYNAMIC_TABLE_SIZE_CHANGE_REQUIRED = IOException("max dynamic table size change required")
        private val EMPTY = byteArrayOf()

        // Unsigned Little Endian Base 128 Variable-Length Integer Encoding
        @Throws(IOException::class)
        private fun decodeULE128(`in`: InputStream): Int {
            `in`.mark(5)
            var result = 0
            var shift = 0
            while (shift < 32) {
                if (`in`.available() == 0) {
                    // Buffer does not contain entire integer,
                    // reset reader index and return -1.
                    `in`.reset()
                    return -1
                }
                val b = `in`.read().toByte()
                if (shift == 28 && b.toInt() and 0xF8 != 0) {
                    break
                }
                result = result or (b.toInt() and 0x7F shl shift)
                if (b.toInt() and 0x80 == 0) {
                    return result
                }
                shift += 7
            }
            // Value exceeds Integer.MAX_VALUE
            `in`.reset()
            throw DECOMPRESSION_EXCEPTION
        }
    }

    /**
     * Creates a new decoder.
     */
    init {
        dynamicTable = DynamicTable(maxHeaderTableSize)
        this.maxHeaderSize = maxHeaderSize
        maxDynamicTableSize = maxHeaderTableSize
        encoderMaxDynamicTableSize = maxHeaderTableSize
        maxDynamicTableSizeChangeRequired = false
        reset()
    }
}