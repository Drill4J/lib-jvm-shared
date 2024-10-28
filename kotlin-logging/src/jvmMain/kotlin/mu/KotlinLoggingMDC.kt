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
package mu

import org.slf4j.MDC

/**
 * Use a pair in MDC context. Example:
 * ```
 * withLoggingContext("userId" to userId) {
 *   doSomething()
 * }
 * ```
 * ```
 * withLoggingContext("userId" to userId, restorePrevious = false) {
 *   doSomething()
 * }
 * ```
 */
public inline fun <T> withLoggingContext(
    pair: Pair<String, String?>,
    restorePrevious: Boolean = true,
    body: () -> T
): T =
    if (pair.second == null) {
        body()
    } else if (!restorePrevious) {
        MDC.putCloseable(pair.first, pair.second).use { body() }
    } else {
        val previousValue = MDC.get(pair.first)
        try {
            MDC.putCloseable(pair.first, pair.second).use { body() }
        } finally {
            if (previousValue != null) MDC.put(pair.first, previousValue)
        }
    }

/**
 * Use a vary number of pairs in MDC context. Example:
 * ```
 * withLoggingContext("userId" to userId) {
 *   doSomething()
 * }
 * ```
 */
public inline fun <T> withLoggingContext(
    vararg pair: Pair<String, String?>,
    restorePrevious: Boolean = true,
    body: () -> T
): T {
    val pairForMDC = pair.filter { it.second != null }
    val cleanupCallbacks = pairForMDC.map { (mdcKey, _) ->
        val mdcValue = MDC.get(mdcKey)
        if (restorePrevious && mdcValue != null) {
            { MDC.put(mdcKey, mdcValue) }
        } else {
            { MDC.remove(mdcKey) }
        }
    }

    try {
        pairForMDC.forEach { MDC.put(it.first, it.second) }
        return body()
    } finally {
        cleanupCallbacks.forEach { it.invoke() }
    }
}

/**
 * Use a map in MDC context. Example:
 * ```
 * withLoggingContext(mapOf("userId" to userId)) {
 *   doSomething()
 * }
 * ```
 * ```
 * withLoggingContext(mapOf("userId" to userId), restorePrevious = true) {
 *   doSomething()
 * }
 * ```
 */
public inline fun <T> withLoggingContext(
    map: Map<String, String?>,
    restorePrevious: Boolean = true,
    body: () -> T
): T {
    val cleanupCallbacks = map.map {
        val mdcValue = MDC.get(it.key)
        if (restorePrevious && mdcValue != null) {
            { MDC.put(it.key, mdcValue) }
        } else {
            { MDC.remove(it.key) }
        }
    }

    try {
        map.forEach {
            if (it.value != null) {
                MDC.put(it.key, it.value)
            }
        }
        return body()
    } finally {
        cleanupCallbacks.forEach { it.invoke() }
    }
}
