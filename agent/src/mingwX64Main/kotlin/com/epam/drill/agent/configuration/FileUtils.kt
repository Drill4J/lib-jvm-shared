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

import kotlinx.cinterop.AutofreeScope
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import platform.posix.FILE
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen

private val processInfoCmd = "wmic process where \"processid='${getpid()}'\" get commandline"

fun AutofreeScope.openPipe(): CPointer<FILE>? {
    val cmdByteVar = processInfoCmd.cstr.getPointer(this)
    val mode = "r".cstr.getPointer(this)
    return popen?.invoke(cmdByteVar, mode)
}

fun CPointer<FILE>?.close() = pclose?.invoke(this)
