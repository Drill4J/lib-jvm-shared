package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.servers.UndertowTransformerObject

actual object UndertowTransformer :
    TransformerObject,
    UndertowTransformerObject(TestHeadersRetriever),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)
