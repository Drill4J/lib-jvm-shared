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
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.core.ByteReadPacket


internal object StaticTable {
    private const val EMPTY = ""

    // Appendix A: Static Table
    // http://tools.ietf.org/html/rfc7541#appendix-A
    private val STATIC_TABLE = arrayOf( /*  1 */
        HeaderField(":authority", EMPTY),  /*  2 */
        HeaderField(":method", "GET"),  /*  3 */
        HeaderField(":method", "POST"),  /*  4 */
        HeaderField(":path", "/"),  /*  5 */
        HeaderField(":path", "/index.html"),  /*  6 */
        HeaderField(":scheme", "http"),  /*  7 */
        HeaderField(":scheme", "https"),  /*  8 */
        HeaderField(":status", "200"),  /*  9 */
        HeaderField(":status", "204"),  /* 10 */
        HeaderField(":status", "206"),  /* 11 */
        HeaderField(":status", "304"),  /* 12 */
        HeaderField(":status", "400"),  /* 13 */
        HeaderField(":status", "404"),  /* 14 */
        HeaderField(":status", "500"),  /* 15 */
        HeaderField("accept-charset", EMPTY),  /* 16 */
        HeaderField("accept-encoding", "gzip, deflate"),  /* 17 */
        HeaderField("accept-language", EMPTY),  /* 18 */
        HeaderField("accept-ranges", EMPTY),  /* 19 */
        HeaderField("accept", EMPTY),  /* 20 */
        HeaderField("access-control-allow-origin", EMPTY),  /* 21 */
        HeaderField("age", EMPTY),  /* 22 */
        HeaderField("allow", EMPTY),  /* 23 */
        HeaderField("authorization", EMPTY),  /* 24 */
        HeaderField("cache-control", EMPTY),  /* 25 */
        HeaderField("content-disposition", EMPTY),  /* 26 */
        HeaderField("content-encoding", EMPTY),  /* 27 */
        HeaderField("content-language", EMPTY),  /* 28 */
        HeaderField("content-length", EMPTY),  /* 29 */
        HeaderField("content-location", EMPTY),  /* 30 */
        HeaderField("content-range", EMPTY),  /* 31 */
        HeaderField("content-type", EMPTY),  /* 32 */
        HeaderField("cookie", EMPTY),  /* 33 */
        HeaderField("date", EMPTY),  /* 34 */
        HeaderField("etag", EMPTY),  /* 35 */
        HeaderField("expect", EMPTY),  /* 36 */
        HeaderField("expires", EMPTY),  /* 37 */
        HeaderField("from", EMPTY),  /* 38 */
        HeaderField("host", EMPTY),  /* 39 */
        HeaderField("if-match", EMPTY),  /* 40 */
        HeaderField("if-modified-since", EMPTY),  /* 41 */
        HeaderField("if-none-match", EMPTY),  /* 42 */
        HeaderField("if-range", EMPTY),  /* 43 */
        HeaderField("if-unmodified-since", EMPTY),  /* 44 */
        HeaderField("last-modified", EMPTY),  /* 45 */
        HeaderField("link", EMPTY),  /* 46 */
        HeaderField("location", EMPTY),  /* 47 */
        HeaderField("max-forwards", EMPTY),  /* 48 */
        HeaderField("proxy-authenticate", EMPTY),  /* 49 */
        HeaderField("proxy-authorization", EMPTY),  /* 50 */
        HeaderField("range", EMPTY),  /* 51 */
        HeaderField("referer", EMPTY),  /* 52 */
        HeaderField("refresh", EMPTY),  /* 53 */
        HeaderField("retry-after", EMPTY),  /* 54 */
        HeaderField("server", EMPTY),  /* 55 */
        HeaderField("set-cookie", EMPTY),  /* 56 */
        HeaderField("strict-transport-security", EMPTY),  /* 57 */
        HeaderField("transfer-encoding", EMPTY),  /* 58 */
        HeaderField("user-agent", EMPTY),  /* 59 */
        HeaderField("vary", EMPTY),  /* 60 */
        HeaderField("via", EMPTY),  /* 61 */
        HeaderField("www-authenticate", EMPTY)
    )
    private val STATIC_INDEX_BY_NAME = createMap()

    /**
     * The number of header fields in the static table.
     */
    val length = STATIC_TABLE.size

    /**
     * Return the header field at the given index value.
     */
    fun getEntry(index: Int): HeaderField {
        return STATIC_TABLE[index - 1]
    }

    /**
     * Returns the lowest index value for the given header field name in the static table.
     * Returns -1 if the header field name is not in the static table.
     */
    fun getIndex(name: ByteArray): Int {
        val nameString = String(name, 0, name.size, Charset.forName("ISO-8859-1"))
        return STATIC_INDEX_BY_NAME[nameString] ?: return -1
    }

    /**
     * Returns the index value for the given header field in the static table.
     * Returns -1 if the header field is not in the static table.
     */
    fun getIndex(name: ByteArray, value: ByteArray?): Int {
        var index = getIndex(name)
        if (index == -1) {
            return -1
        }

        // Note this assumes all entries for a given header field are sequential.
        while (index <= length) {
            val entry = getEntry(index)
            if (!HpackUtil.equals(name, entry.name)) {
                break
            }
            if (HpackUtil.equals(value, entry.value)) {
                return index
            }
            index++
        }
        return -1
    }

    // create a map of header name to index value to allow quick lookup
    private fun createMap(): Map<String, Int> {
        val length = STATIC_TABLE.size
        val ret = HashMap<String, Int>(length)
        // Iterate through the static table in reverse order to
        // save the smallest index for a given name in the map.
        for (index in length downTo 1) {
            val entry = getEntry(index)

            val name = Charsets.ISO_8859_1.newDecoder().decode(ByteReadPacket(entry.name!!))
            ret[name] = index
        }
        return ret
    }
}

fun String(bytes: ByteArray, s: Int, w: Int, charset: Charset): String {
    return charset.newDecoder().decode(ByteReadPacket(bytes))
}