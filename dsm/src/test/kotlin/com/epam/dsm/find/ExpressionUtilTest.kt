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
package com.epam.dsm.find

import com.epam.dsm.*
import com.epam.dsm.util.*
import kotlin.test.*
import kotlin.test.Test

class ExpressionUtilTest {

    @Test
    fun `should create sql path to field when 1 field`() {
        val field = "nestedObject"
        assertEquals(
            "$JSON_COLUMN ->> ${field.toQuotes()}",
            FieldPath(field).run {
                this.extractText()
            }
        )
    }

    @Test
    fun `should create sql path to field when nested 2 field`() {
        val field = "nestedObject"
        val field2 = "data"
        assertEquals(
            "$JSON_COLUMN -> ${field.toQuotes()} ->> ${field2.toQuotes()}",
            FieldPath(field, field2).run {
                this.extractText()
            }
        )
    }

    @Test
    fun `should create sql path to field when nested three fields`() {
        val field = "details"
        val field2 = "params"
        val field3 = "smth"

        assertEquals(
            "$JSON_COLUMN -> ${field.toQuotes()} -> ${field2.toQuotes()} ->> ${field3.toQuotes()}",
            FieldPath(field, field2, field3).run {
                this.extractText()
            }
        )
    }
}

