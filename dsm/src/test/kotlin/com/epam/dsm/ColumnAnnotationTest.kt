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
package com.epam.dsm

import com.epam.dsm.common.*
import com.epam.dsm.find.*
import com.epam.dsm.util.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.reflect.full.*
import kotlin.test.*

class ColumnAnnotationTest : PostgresBased("separate_column") {

    @Test
    fun `annotation in simple class - should detect Column annotation`() {
        val columnPaths = Simple::class.serializer().descriptor.findColumnAnnotation()
        assertTrue { columnPaths.isNotEmpty() }
        assertTrue { columnPaths.containsValue(listOf(Simple::separate.name)) }
    }

    @Test
    fun `annotation in inner class - should detect path`() {
        val columnPaths = Complex::class.serializer().descriptor.findColumnAnnotation()
        assertTrue { columnPaths.isNotEmpty() }
        assertTrue { columnPaths.containsValue(listOf(Complex::simple.name, Simple::separate.name)) }
    }

    @Test
    fun `should filter by field annotated with Column`(): Unit = runBlocking {
        val id = "some_id"
        val simple = Simple("id", SimpleObject("id", "test", 123, Last(1)))
        val complex = Complex(id, simple, 123)
        storeClient.store(complex)
        val result = storeClient.findBy<Complex> {
            ColumnPath(Simple::separate, SimpleObject::int) eq "123"
        }.get()
        assertNotNull(result)
    }
}

@Serializable
data class Simple(
    val id: String,
    @Column(name = "separate", type = PostgresType.JSONB)
    val separate: SimpleObject
)

@Serializable
data class Complex(
    @Id val id: String,
    val simple: Simple,
    val int: Int
)

