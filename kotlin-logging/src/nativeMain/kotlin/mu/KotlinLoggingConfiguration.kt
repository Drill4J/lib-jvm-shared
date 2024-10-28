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
package mu

import kotlin.concurrent.AtomicReference

public expect val DefaultAppender: Appender

@Suppress("ObjectPropertyName")
public object KotlinLoggingConfiguration {
    private val _logLevel = AtomicReference(KotlinLoggingLevel.INFO)
    public var logLevel: KotlinLoggingLevel
        get() = _logLevel.value
        set(value) {
            _logLevel.value = value
        }
    private val _appender = AtomicReference<Appender>(DefaultAppender)
    public var appender: Appender
        get() = _appender.value
        set(value) {
            _appender.value = value
        }
    private val _formatter = AtomicReference<Formatter>(DefaultMessageFormatter)
    public var formatter: Formatter
        get() = _formatter.value
        set(value) {
            _formatter.value = value
        }
}
