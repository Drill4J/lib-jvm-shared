package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TestHeadersRetriever
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.instrument.servers.TomcatTransformerObject

object TomcatTransformer :
    TomcatTransformerObject(TestHeadersRetriever),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)
