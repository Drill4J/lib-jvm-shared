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
package com.epam.drill.logger.api

import kotlin.test.*

class LoggerTest {

    private val appender = TestAppender()

    @Test
    fun `log trace`() {
        val loggerName = "dummy"
        val logger = loggerName.namedLogger({ LogLevel.TRACE }, appender)
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }
        val records = listOf(
            LogRecord(name = loggerName, level = LogLevel.TRACE, msg = "trace"),
            LogRecord(name = loggerName, level = LogLevel.DEBUG, msg = "debug"),
            LogRecord(name = loggerName, level = LogLevel.INFO, msg = "info"),
            LogRecord(name = loggerName, level = LogLevel.WARN, msg = "warn"),
            LogRecord(name = loggerName, level = LogLevel.ERROR, msg = "error")
        )
        assertEquals(records, appender.records)
    }

    @Test
    fun `log debug`() {
        val loggerName = "dummy"
        val logger = loggerName.namedLogger({ LogLevel.DEBUG }, appender)
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }
        val records = listOf(
            LogRecord(name = loggerName, level = LogLevel.DEBUG, msg = "debug"),
            LogRecord(name = loggerName, level = LogLevel.INFO, msg = "info"),
            LogRecord(name = loggerName, level = LogLevel.WARN, msg = "warn"),
            LogRecord(name = loggerName, level = LogLevel.ERROR, msg = "error")
        )
        assertEquals(records, appender.records)
    }

    @Test
    fun `log info`() {
        val loggerName = "dummy"
        val logger = loggerName.namedLogger({ LogLevel.INFO }, appender)
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }
        val records = listOf(
            LogRecord(name = loggerName, level = LogLevel.INFO, msg = "info"),
            LogRecord(name = loggerName, level = LogLevel.WARN, msg = "warn"),
            LogRecord(name = loggerName, level = LogLevel.ERROR, msg = "error")
        )
        assertEquals(records, appender.records)
    }

    @Test
    fun `log warn`() {
        val loggerName = "dummy"
        val logger = loggerName.namedLogger({ LogLevel.WARN }, appender)
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }
        val records = listOf(
            LogRecord(name = loggerName, level = LogLevel.WARN, msg = "warn"),
            LogRecord(name = loggerName, level = LogLevel.ERROR, msg = "error")
        )
        assertEquals(records, appender.records)
    }

    @Test
    fun `log error`() {
        val loggerName = "dummy"
        val logger = loggerName.namedLogger({ LogLevel.ERROR }, appender)
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }
        val records = listOf(
            LogRecord(name = loggerName, level = LogLevel.ERROR, msg = "error")
        )
        assertEquals(records, appender.records)
    }
}

private data class LogRecord(
    val name: String,
    val level: LogLevel,
    val t: Throwable? = null,
    val marker: Marker? = null,
    val msg: Any?
)

private class TestAppender : LogAppender {
    val records = mutableListOf<LogRecord>()
    override fun invoke(
        name: String,
        level: LogLevel,
        t: Throwable?,
        marker: Marker?,
        msg: () -> Any?
    ) = records.add(
        LogRecord(
            name = name,
            level = level,
            t = t,
            marker = marker,
            msg = msg()
        )
    ).run {}
}
