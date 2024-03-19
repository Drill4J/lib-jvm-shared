package com.epam.drill.agent.instrument.transformers.clients

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.clients.WebClientTransformerObject

actual object WebClientTransformer:
    TransformerObject,
    WebClientTransformerObject(),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder),
    ClassPathProvider by TestClassPathProvider