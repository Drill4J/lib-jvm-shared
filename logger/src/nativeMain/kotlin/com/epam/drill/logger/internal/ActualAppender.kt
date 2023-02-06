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
package com.epam.drill.logger.internal

import io.ktor.util.date.*
import io.ktor.utils.io.streams.*
import platform.posix.*
import kotlin.native.concurrent.*

internal actual val platformName: String = Platform.osFamily.name

@SharedImmutable
internal val _fileDescriptor = AtomicReference<Int?>(null)

internal fun output(block: (Appendable) -> Unit) {
    _fileDescriptor.value?.let {
        Output(STDOUT_FILENO).flush()
        val out = Output(it)
        block(out)
        out.flush()
    } ?: block(StdOut)
}

private object StdOut : Appendable {
    override fun append(value: Char): Appendable = apply { print(value) }

    override fun append(value: CharSequence?): Appendable = apply { print(value) }

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable = apply {
        print(value?.subSequence(startIndex, endIndex))
    }
}

actual object Calendar {
    actual fun timestamp(): String = GMTDate().run {
        "$year-${(month.ordinal.inc()).padStart(2)}-${dayOfMonth.padStart(2)} ${hours.padStart(2)}:${minutes.padStart(2)}:${seconds.padStart(2)}"
    }

    private fun Int.padStart(length: Int): String = toString().padStart(length, '0')
}
