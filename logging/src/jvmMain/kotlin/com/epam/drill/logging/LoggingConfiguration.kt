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
package com.epam.drill.logging

import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.LogManager

actual object LoggingConfiguration {

    private val levelMapping = mapOf(
        "TRACE" to "FINEST",
        "DEBUG" to "FINE",
        "INFO" to "INFO",
        "WARN" to "WARNING",
        "ERROR" to "SEVERE"
    )

    private var filename: String? = null

    actual fun readDefaultConfiguration() {
        ClassLoader.getSystemResourceAsStream("logging.properties").use(LogManager.getLogManager()::readConfiguration)
    }

    actual fun setLoggingLevels(levels: List<Pair<String, String>>) {
        val levelRegex = Regex("(TRACE|DEBUG|INFO|WARN|ERROR)")
        val isCorrect: (Pair<String, String>) -> Boolean = { levelRegex.matches(it.second) }
        levels.filter(isCorrect).forEach {
            Logger.getLogger(it.first).level = Level.parse(levelMapping[it.second])
        }
    }

    actual fun setLoggingLevels(levels: String) {
        val levelPairRegex = Regex("([\\w.]*=)?(TRACE|DEBUG|INFO|WARN|ERROR)")
        val toLevelPair: (String) -> Pair<String, String>? = { str ->
            str.takeIf(levelPairRegex::matches)?.let { it.substringBefore("=", "") to it.substringAfter("=") }
        }
        setLoggingLevels(levels.split(";").mapNotNull(toLevelPair))
    }

    actual fun setLoggingFilename(filename: String?) {
        val handler: Handler = filename?.runCatching(::FileHandler)?.getOrNull() ?: ConsoleHandler()
        val nameToLogger: (String) -> Logger = LogManager.getLogManager()::getLogger
        val withHandlers: (Logger) -> Boolean = { it.handlers.isNotEmpty() }
        LogManager.getLogManager().loggerNames.toList().map(nameToLogger).filter(withHandlers).forEach {
            it.handlers.filterIsInstance(FileHandler::class.java).forEach { it.runCatching(FileHandler::close) }
            it.handlers.forEach(it::removeHandler)
            it.addHandler(handler)
        }
        this.filename = filename
    }

    actual fun getLoggingFilename(): String? = filename

}
