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
package com.epam.drill.agent.configuration

import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input

actual fun drillInstallationDir() = run {
    val isContainsAgentPath: (String) -> Boolean = { it.contains("-agentpath:") }
    val fromEnv: () -> String? = { getenv("JAVA_TOOL_OPTIONS")?.toKString() }
    val agentLine = fromEnv()?.takeIf(isContainsAgentPath) ?: fromWmic()?.takeIf(isContainsAgentPath)
    agentLine?.let { Regex("-agentpath:(.+?)($|=.+)").matchEntire(agentLine)!!.groups[1]!!.value.substringBeforeLast("\\") }
}

private fun fromWmic() = memScoped {
    val pid = getpid()
    val command = "wmic process where \"processid='$pid'\" get commandline"
    val file = popen?.invoke(command.cstr.getPointer(this), "r".cstr.getPointer(this))
    file?.let { Input(file).readText().also { pclose?.invoke(file) } }
}
