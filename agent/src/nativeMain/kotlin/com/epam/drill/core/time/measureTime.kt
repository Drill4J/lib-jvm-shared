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
package com.epam.drill.core.time

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.getTimeMillis

data class TimedValue<T>(val value: T, val duration: Duration)

inline fun measureTime(block: () -> Unit): Duration {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val mark = getTimeMillis()
    block()
    val now = getTimeMillis()
    return now - mark
}

inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val mark = getTimeMillis()
    val result = block()
    val now = getTimeMillis()
    return TimedValue(result, now - mark)
}
