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

import com.epam.drill.ts.kt2dts.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.*
import java.net.*


fun main(args: Array<String>) = Kt2DtsMain().main(args)

private class Kt2DtsMain : CliktCommand() {
    private val cp: List<String>? by option(help = "Classpath (comma separated)").split(",")
    private val module: String by option(help = "Module name").default("example")
    private val output: String? by option(help = "Output file, if not specified stdout is used")

    override fun run() {
        val classLoader = cp?.run {
            val urls = map { File(it).toURI().toURL() }
            println("Converting classes from $urls")
            URLClassLoader(urls.toTypedArray(), Thread.currentThread().contextClassLoader)
        }
        val converted = findSerialDescriptors(classLoader).convert()
        output?.let {
            File(it).bufferedWriter().use { writer ->
                converted.appendTo(writer, module)
            }
        } ?: converted.appendTo(System.out, module)
    }
}
