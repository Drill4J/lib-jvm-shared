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
package com.epam.drill.agent.instrument

import mu.KotlinLogging

object TransformerRegistrar {
    private val logger = KotlinLogging.logger("com.epam.drill.agent.instrument.TransformerRegistrar")
    private lateinit var transformers: Set<Transformer>
    val enabledTransformers by lazy {
        transformers.filter { it.enabled() }
    }

    fun initialize(transformers: Set<Transformer>) {
        this.transformers = transformers
        logger.info { "Enabled ${enabledTransformers.size} transformers" }
    }
}