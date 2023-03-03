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
package com.epam.drill.ts.kt2dts

import kotlin.test.*


class ConverterTest {
    @Test
    fun `converts Sample class correctly`() {
        val descriptors = sequenceOf(
            Sample::class.descriptor()
        )
        val tsInterface = descriptors.convert().first() as TsInterface
        assertEquals(Sample::class.simpleName, tsInterface.name)
        tsInterface.fields[0].run {
            assertEquals("num?", name)
            assertEquals("number", type)
        }
        tsInterface.fields[1].run {
            assertEquals("str", name)
            assertEquals("string | null", type)
        }
    }

    @Test
    fun `converts Complex class correctly`() {
        val expected = TsInterface(
            name = Complex::class.simpleName!!,
            fields = listOf(
                TsField(Complex::num.name, "number"),
                TsField(Complex::list.name, "string[]"),
                TsField(Complex::optList.name, "(string | null)[]"),
                TsField(Complex::listOfLists.name, "Sample[][]"),
                TsField(Complex::listOfOptLists.name, "(Sample[] | null)[]"),
                TsField(Complex::listOfListsOpt.name, "((Sample | null)[])[]"),
                TsField(Complex::mapOfLists.name, "{ [key: string]: string[] }"),
                TsField(Complex::mapOfOptLists.name, "{ [key: string]: string[] | null }")
            )
        )
        val descriptors = sequenceOf(
            Complex::class.descriptor()
        )
        val converted = descriptors.convert().first()
        assertEquals(expected.render(), converted.render())
    }
}
