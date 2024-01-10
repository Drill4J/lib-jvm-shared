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
package com.epam.drill.jvmapi

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import com.epam.drill.jvmapi.gen.GetArrayLength
import com.epam.drill.jvmapi.gen.GetPrimitiveArrayCritical
import com.epam.drill.jvmapi.gen.JNI_ABORT
import com.epam.drill.jvmapi.gen.NewByteArray
import com.epam.drill.jvmapi.gen.ReleasePrimitiveArrayCritical
import com.epam.drill.jvmapi.gen.SetByteArrayRegion
import com.epam.drill.jvmapi.gen.jobject

fun toJByteArray(array: ByteArray) = NewByteArray(array.size)!!.apply {
    array.usePinned { SetByteArrayRegion(this, 0, array.size, it.addressOf(0)) }
}

fun toByteArray(jarray: jobject) = ByteArray(GetArrayLength(jarray)).apply {
    if (this.isEmpty()) return@apply
    val buffer = GetPrimitiveArrayCritical(jarray, null)
    try {
        this.usePinned { memcpy(it.addressOf(0), buffer, this.size.convert()) }
    } finally {
        ReleasePrimitiveArrayCritical(jarray, buffer, JNI_ABORT)
    }
}
