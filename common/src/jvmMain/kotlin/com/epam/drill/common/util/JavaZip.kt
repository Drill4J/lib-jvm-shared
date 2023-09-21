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
package com.epam.drill.common.util

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object JavaZip {

    fun compress(input: ByteArray, level: Int = 1): ByteArray = Deflater(level, true).run {
        ByteArrayOutputStream().use { stream ->
            this.setInput(input)
            this.finish()
            val readBuffer = ByteArray(1024)
            val readed: (Int) -> Boolean = { it > 0 }
            while (!this.finished()) {
                this.deflate(readBuffer).takeIf(readed)?.also { stream.write(readBuffer, 0, it) }
            }
            this.end()
            stream.toByteArray()
        }
    }

    fun decompress(input: ByteArray): ByteArray = Inflater(true).run {
        ByteArrayOutputStream().use { stream ->
            this.setInput(input)
            val readBuffer = ByteArray(1024)
            val readed: (Int) -> Boolean = { it > 0 }
            while (!this.finished()) {
                this.inflate(readBuffer).takeIf(readed)?.also { stream.write(readBuffer, 0, it) }
            }
            this.end()
            stream.toByteArray()
        }
    }

}
