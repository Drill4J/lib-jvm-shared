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
package com.epam.drill.agent.transport.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HttpResponseStatusTest {

    @Test
    fun `success for HTTP 200`() {
        val status = HttpResponseStatus(200)
        assertTrue(status.success)
        assertEquals(200, status.statusObject)
    }

    @Test
    fun `failed for HTTP 500`() {
        val status = HttpResponseStatus(500)
        assertFalse(status.success)
        assertEquals(500, status.statusObject)
    }

}
