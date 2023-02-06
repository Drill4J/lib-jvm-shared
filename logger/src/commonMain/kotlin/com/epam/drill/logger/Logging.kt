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
package com.epam.drill.logger

import com.epam.drill.logger.api.*
import com.epam.drill.logger.internal.*

object Logging : LoggerFactory {
    fun logger(func: () -> Unit): Logger {
        val name = func::class.qualifiedName?.replace(".${func::class.simpleName}", "")
        return logger(name ?: "unknown logger")
    }

    override fun logger(name: String): Logger = name.namedLogger(::logLevel, ::appendLogMessage)
}

expect var Logging.logLevel: LogLevel

expect var Logging.filename: String?

expect fun Logging.output(message: String)
