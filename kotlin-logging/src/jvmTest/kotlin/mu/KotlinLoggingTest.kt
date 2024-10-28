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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

private val logger = KotlinLogging.logger { }
private val loggerFromSlf4j = KotlinLogging.logger(KLoggingContext.context.getLogger("mu.slf4jLogger"))
private val loggerFromSlf4jExtension = KLoggingContext.context.getLogger("mu.slf4jLoggerExtension").toKLogger()

class ForKotlinLoggingTest {
    val loggerInClass = KotlinLogging.logger { }

    companion object {
        val loggerInCompanion = KotlinLogging.logger { }
    }
}

class KotlinLoggingTest {

    @Test
    fun testLoggerName() {
        assertAll(
            { assertEquals("mu.KotlinLoggingTest", logger.name) },
            { assertEquals("mu.ForKotlinLoggingTest", ForKotlinLoggingTest().loggerInClass.name) },
            { assertEquals("mu.ForKotlinLoggingTest", ForKotlinLoggingTest.loggerInCompanion.name) },
            { assertEquals("mu.slf4jLogger", loggerFromSlf4j.name) },
            { assertEquals("mu.slf4jLoggerExtension", loggerFromSlf4jExtension.name) },
        )
    }
}
