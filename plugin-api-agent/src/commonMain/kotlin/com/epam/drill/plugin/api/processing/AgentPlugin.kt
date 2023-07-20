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
package com.epam.drill.plugin.api.processing

import com.epam.drill.common.classloading.EntitySource

interface AgentContext {
    operator fun invoke(): String?
    operator fun get(key: String): String?
}

interface Sender {
    fun send(pluginId: String, message: String)
}

interface AgentPlugin<A> : DrillPlugin<A>, Switchable

interface DrillPlugin<A> {
    suspend fun doAction(action: A): Any
    fun parseAction(rawAction: String): A
    suspend fun doRawAction(rawAction: String): Any = doAction(parseAction(rawAction))
}

interface Switchable {
    fun on()
    fun off()
}

interface Instrumenter {
    fun instrument(className: String, initialBytes: ByteArray): ByteArray?
}

/**
 * Service for scanning classes of the application under test
 */
interface ClassScanner {
    /**
     * Scan target classes of the application under test
     * @param consumer the function for processing chunks of scanned classes
     */
    fun scanClasses(consumer: (Set<EntitySource>) -> Unit)
}
