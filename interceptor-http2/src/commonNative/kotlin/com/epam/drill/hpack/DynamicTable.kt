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

internal class DynamicTable(initialCapacity: Int) {
    // a circular queue of header fields
    var headerFields: Array<HeaderField?>? =null
    var head = 0
    var tail = 0
    private var size = 0
    private var capacity = -1 // ensure setCapacity creates the array

    fun length(): Int {
        val length: Int
        length = if (head < tail) {
            headerFields!!.size - tail + head
        } else {
            head - tail
        }
        return length
    }

    fun size(): Int {
        return size
    }

    fun capacity(): Int {
        return capacity
    }

    fun getEntry(index: Int): HeaderField? {
        if (index <= 0 || index > length()) {
            throw IndexOutOfBoundsException()
        }
        val i = head - index
        return if (i < 0) {
            headerFields!![i + headerFields!!.size]
        } else {
            headerFields!![i]
        }
    }

    /**
     * Add the header field to the dynamic table.
     * Entries are evicted from the dynamic table until the size of the table
     * and the new header field is less than or equal to the table's capacity.
     * If the size of the new entry is larger than the table's capacity,
     * the dynamic table will be cleared.
     */
    fun add(header: HeaderField) {
        val headerSize = header.size()
        if (headerSize > capacity) {
            clear()
            return
        }
        while (size + headerSize > capacity) {
            remove()
        }
        headerFields!![head++] = header
        size += header.size()
        if (head == headerFields!!.size) {
            head = 0
        }
    }

    /**
     * Remove and return the oldest header field from the dynamic table.
     */
    fun remove(): HeaderField? {
        val removed = headerFields!![tail] ?: return null
        size -= removed.size()
        headerFields!![tail++] = null
        if (tail == headerFields!!.size) {
            tail = 0
        }
        return removed
    }

    /**
     * Remove all entries from the dynamic table.
     */
    fun clear() {
        while (tail != head) {
            headerFields!![tail++] = null
            if (tail == headerFields!!.size) {
                tail = 0
            }
        }
        head = 0
        tail = 0
        size = 0
    }

    /**
     * Set the maximum size of the dynamic table.
     * Entries are evicted from the dynamic table until the size of the table
     * is less than or equal to the maximum size.
     */
    fun setCapacity(capacity: Int) {
        require(capacity >= 0) { "Illegal Capacity: $capacity" }

        // initially capacity will be -1 so init won't return here
        if (this.capacity == capacity) {
            return
        }
        this.capacity = capacity
        if (capacity == 0) {
            clear()
        } else {
            // initially size will be 0 so remove won't be called
            while (size > capacity) {
                remove()
            }
        }
        var maxEntries: Int = capacity / HeaderField.Companion.HEADER_ENTRY_OVERHEAD
        if (capacity % HeaderField.Companion.HEADER_ENTRY_OVERHEAD != 0) {
            maxEntries++
        }

        // check if capacity change requires us to reallocate the array
        if (headerFields != null && headerFields!!.size == maxEntries) {
            return
        }
        val tmp = arrayOfNulls<HeaderField>(maxEntries)

        // initially length will be 0 so there will be no copy
        val len = length()
        var cursor = tail
        for (i in 0 until len) {
            val entry = headerFields!![cursor++]
            tmp[i] = entry
            if (cursor == headerFields!!.size) {
                cursor = 0
            }
        }
        tail = 0
        head = tail + len
        headerFields = tmp
    }

    /**
     * Creates a new dynamic table with the specified initial capacity.
     */
    init {
        setCapacity(initialCapacity)
    }
}