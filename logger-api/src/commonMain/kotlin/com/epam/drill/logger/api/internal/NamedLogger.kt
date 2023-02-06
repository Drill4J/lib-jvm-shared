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
package com.epam.drill.logger.api.internal

import com.epam.drill.logger.api.*

internal class NamedLogger(
    val name: String,
    private val logLevel: () -> LogLevel = { LogLevel.ERROR },
    private val appender: LogAppender
) : Logger {
    override fun trace(t: Throwable?, marker: Marker?, msg: () -> Any?) {
        log(LogLevel.TRACE, t = t, marker = marker, msg = msg)
    }

    override fun debug(t: Throwable?, marker: Marker?, msg: () -> Any?) {
        log(LogLevel.DEBUG, t = t, marker = marker, msg = msg)
    }

    override fun info(t: Throwable?, marker: Marker?, msg: () -> Any?) {
        log(LogLevel.INFO, t = t, marker = marker, msg = msg)
    }

    override fun warn(t: Throwable?, marker: Marker?, msg: () -> Any?) {
        log(LogLevel.WARN, t = t, marker = marker, msg = msg)
    }

    override fun error(t: Throwable?, marker: Marker?, msg: () -> Any?) {
        log(LogLevel.ERROR, t = t, marker = marker, msg = msg)
    }

    private fun log(
        level: LogLevel,
        t: Throwable? = null,
        marker: Marker? = null,
        msg: () -> Any?
    ) {
        if (logLevel() <= level) {
            appender(name, level, t, marker, msg)
        }
    }
}
