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

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.encodeToByteArray
import kotlin.math.min


fun String.toByteArray(charset: Charset):ByteArray{
    return charset.newEncoder().encodeToByteArray(this)
}

open class HeaderField(name: ByteArray?, value: ByteArray?) : Comparable<HeaderField> {

    val name: ByteArray?

    val value: ByteArray?

    // This constructor can only be used if name and value are ISO-8859-1 encoded.
    constructor(name: String, value: String) : this(name.toByteArray(Charset.forName("ISO-8859-1")), value.toByteArray(Charset.forName("ISO-8859-1"))) {}

    fun size(): Int {
        return name!!.size + value!!.size + HEADER_ENTRY_OVERHEAD
    }

    override fun compareTo(anotherHeaderField: HeaderField): Int {
        var ret = compareTo(name, anotherHeaderField.name)
        if (ret == 0) {
            ret = compareTo(value, anotherHeaderField.value)
        }
        return ret
    }

    private fun compareTo(s1: ByteArray?, s2: ByteArray?): Int {
        val len1 = s1!!.size
        val len2 = s2!!.size
        val lim = min(len1, len2)
        var k = 0
        while (k < lim) {
            val b1 = s1[k]
            val b2 = s2[k]
            if (b1 != b2) {
                return b1 - b2
            }
            k++
        }
        return len1 - len2
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is HeaderField) {
            return false
        }
        val other = obj
        val nameEquals = HpackUtil.equals(name, other.name)
        val valueEquals = HpackUtil.equals(value, other.value)
        return nameEquals && valueEquals
    }

    override fun toString(): String {
        val nameString = name!!.decodeToString()
        val valueString = value!!.decodeToString()
        return "$nameString: $valueString"
    }

    companion object {
        // Section 4.1. Calculating Table Size
        // The additional 32 octets account for an estimated
        // overhead associated with the structure.
        const val HEADER_ENTRY_OVERHEAD = 32
        fun sizeOf(name: ByteArray, value: ByteArray): Int {
            return name.size + value.size + HEADER_ENTRY_OVERHEAD
        }
    }

    init {
        this.name = HpackUtil.requireNonNull(name)
        this.value = HpackUtil.requireNonNull(value)
    }
}