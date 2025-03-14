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
package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.servers.ReactorTransformerObject
import com.epam.drill.agent.instrument.transformers.reactor.FluxTransformer
import com.epam.drill.agent.instrument.transformers.reactor.MonoTransformer
import com.epam.drill.agent.instrument.transformers.reactor.ParallelFluxTransformer
import com.epam.drill.agent.instrument.transformers.reactor.SchedulersTransformer
import com.epam.drill.agent.instrument.transformers.reactor.TaskExecutorTransformer
import com.epam.drill.agent.common.request.RequestHolder

actual object ReactorTransformer :
    TransformerObject,
    ReactorTransformerObject(
        setOf(
            FluxTransformer, MonoTransformer, ParallelFluxTransformer, SchedulersTransformer, TaskExecutorTransformer
        )
    ),
    RequestHolder by TestRequestHolder,
    ClassPathProvider by TestClassPathProvider
