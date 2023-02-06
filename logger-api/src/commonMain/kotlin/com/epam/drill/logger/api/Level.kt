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

enum class LogLevel(val code: Int) {
    TRACE(0),
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40);
}

fun Int.toLogLevel(): LogLevel {
    for (level in LogLevel.values()) {
        if (level.code == this) {
            return level
        }
    }
    return LogLevel.ERROR
}
