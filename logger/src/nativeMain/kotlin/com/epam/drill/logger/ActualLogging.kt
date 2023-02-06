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
@file:Suppress("ObjectPropertyName")

package com.epam.drill.logger

import com.epam.drill.logger.api.*
import com.epam.drill.logger.internal.*
import io.ktor.utils.io.streams.*
import platform.posix.*
import kotlin.native.concurrent.*

@SharedImmutable
private val _logLevel = AtomicReference(LogLevel.ERROR)

@SharedImmutable
private val _filename = AtomicReference<String?>(null)

actual var Logging.logLevel: LogLevel
    get() = _logLevel.value
    set(value) {
        _logLevel.value = value
    }

actual var Logging.filename: String?
    get() = _filename.value
    set(value) {
        _fileDescriptor.value?.let { Output(it).close() }
        val descriptor = value?.let { f ->
            open(
                f,
                O_WRONLY or O_CREAT or O_TRUNC,
                S_IRUSR or S_IWUSR or S_IRGRP or S_IROTH
            ).takeIf { it > STDERR_FILENO }
        }
        _fileDescriptor.value = descriptor
        _filename.value = descriptor?.let { value.freeze() }
    }

actual fun Logging.output(message: String) {
    output { it.append(message).append('\n') }
}
