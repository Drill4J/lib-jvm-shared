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
package com.epam.dsm.util

import java.util.*
import java.util.stream.*

fun BitSet.stringRepresentation(): String {
    val size = length() - 1 //bitset issue.
    return IntStream
        .range(0, size)
        .mapToObj { i: Int -> if (get(i)) '1' else '0' }
        .collect(
            { StringBuilder(size) },
            { buffer: StringBuilder, characterToAdd: Char -> buffer.append(characterToAdd) },
            { obj: StringBuilder, s: StringBuilder -> obj.append(s) })
        .toString()
}

fun String.toBitSet(): BitSet {
    val bitSet = BitSet(length + 1)
    bitSet.set(length)//magic
    forEachIndexed { inx, ch ->
        if (ch == '1') {
            bitSet.set(inx)
        }
    }
    return bitSet
}
