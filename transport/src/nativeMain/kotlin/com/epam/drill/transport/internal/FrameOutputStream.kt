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
package com.epam.drill.transport.internal

import kotlinx.atomicfu.atomic
import com.epam.drill.websocket.gen.LWS_PRE

private const val BUFF_SIZE_MAX_VALUE = 1024 * 100000

internal class FrameOutputStream(private val byteArray: ByteArray, private val txSize: Int = BUFF_SIZE_MAX_VALUE) {

    private var currentIndex = atomic(0)

    fun readNext(): Frame {
        val isFirstFrame = currentIndex.value == 0
        val endIndex = minOf(currentIndex.value + txSize, byteArray.size)
        val frameContent = byteArray.copyOfRange(currentIndex.value, endIndex)
        currentIndex.value = endIndex
        return Frame(ByteArray(LWS_PRE) + frameContent, isFirstFrame, currentIndex.value == byteArray.size)

    }

    fun isFinish(): Boolean {
        return currentIndex.value >= byteArray.size
    }

}
