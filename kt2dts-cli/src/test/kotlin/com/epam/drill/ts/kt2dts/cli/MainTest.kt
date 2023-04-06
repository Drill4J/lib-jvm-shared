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
package com.epam.drill.ts.kt2dts.cli

import java.io.*
import kotlin.test.*

class MainTest {
    @Test
    fun `no classes to convert`() {
        main(arrayOf())
    }

    @Test
    fun `converts classes from sample-api jar without errors`() {
        val module = "kt2dts-api-sample"
        val rootDir = File("..", module)
        val jarFile = rootDir.walkTopDown().first { it.name.endsWith(".jar") }
        val file = File.createTempFile("kt2dts-", "-api-sample.d.ts")
        println(file)
        try {
            val args = listOf(
                "--cp=${jarFile.path}",
                "--module=$module",
                "--output=${file.path}"
            )
            main(args.toTypedArray())
            assertTrue("Output file is empty") { file.length() > 0 }
        } finally {
            file.delete()
        }
    }
}
