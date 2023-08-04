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
package com.epam.drill.core.ws

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*

actual suspend fun fileWrite(file: CPointer<FILE>, position: Long, data: ByteArray): Long {
    data class Info(val file: CPointer<FILE>, val position: Long, val data: ByteArray)

    if (data.isEmpty()) return 0L

    return executeInWorker(
        IOWorker,
        Info(file, position, if (data.isFrozen) data else data.copyOf())
    ) { (fd, position, data) ->
        data.usePinned { pin ->
            fseek(fd, position.convert(), SEEK_SET)
            fwrite(pin.addressOf(0), 1.convert(), data.size.convert(), fd).toLong()
        }.toLong()
    }
}

actual suspend fun fileSetLength(file: String, length: Long) {
    data class Info(val file: String, val length: Long)

    return executeInWorker(
        IOWorker,
        Info(file, length)
    ) { (fd, len) ->
        truncate(fd, len.convert())
        Unit
    }
}

actual suspend fun fileLength(file: CPointer<FILE>): Long = executeInWorker(
    IOWorker,
    file
) { fd ->
    val prev = ftell(fd)
    fseek(fd, 0L.convert(), SEEK_END)
    val end = ftell(fd)
    fseek(fd, prev.convert(), SEEK_SET)
    end
}
