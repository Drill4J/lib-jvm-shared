package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.servers.ReactorTransformerObject
import com.epam.drill.agent.instrument.transformers.reactor.FluxTransformer
import com.epam.drill.agent.instrument.transformers.reactor.MonoTransformer
import com.epam.drill.agent.instrument.transformers.reactor.ParallelFluxTransformer
import com.epam.drill.agent.instrument.transformers.reactor.SchedulersTransformer
import com.epam.drill.common.agent.request.RequestHolder

actual object ReactorTransformer :
    TransformerObject,
    ReactorTransformerObject(
        setOf(
            FluxTransformer, MonoTransformer, ParallelFluxTransformer, SchedulersTransformer
        )
    ),
    RequestHolder by TestRequestHolder,
    ClassPathProvider by TestClassPathProvider
