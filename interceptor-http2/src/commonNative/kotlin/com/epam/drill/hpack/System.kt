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

object System {

    fun <T> arraycopy(src: Array<T?>?, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(src: ByteArray?, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(src: IntArray?, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)

    }


}
