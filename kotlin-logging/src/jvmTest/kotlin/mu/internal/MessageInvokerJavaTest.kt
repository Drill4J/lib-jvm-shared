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
package mu.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MessageInvokerJavaTest {

    @Test
    fun toStringSafeChecks() {
        assertEquals("hi", { "hi" }.toStringSafe())
    }

    @Test
    fun toStringSafeChecksThrowException() {
        assertEquals("Log message invocation failed: java.lang.Exception: hi", { throw Exception("hi") }.toStringSafe())
    }

    @Test
    fun toStringSafeChecksThrowExceptionWithSystemProperty() {
        assertThrows<Exception> {
            System.setProperty("kotlin-logging.throwOnMessageError", "")
            try {
                { throw Exception("hi") }.toStringSafe()
            } finally {
                System.clearProperty("kotlin-logging.throwOnMessageError")
            }
        }
    }
}
