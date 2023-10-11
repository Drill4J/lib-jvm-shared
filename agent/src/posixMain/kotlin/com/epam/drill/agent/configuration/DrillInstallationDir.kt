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

import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input
import kotlinx.cinterop.memScoped


actual fun getAgentPathCommandFromProcess(): String? = fromProc()

private fun fromProc() = memScoped {
    val file = open("/proc/self/cmdline", O_RDONLY)
    Input(file).readText().also { close(file) }
}
