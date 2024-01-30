package com.epam.drill.agent.instrument.clients

import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TestHeadersRetriever
import com.epam.drill.agent.instrument.TestRequestHolder

object JavaHttpUrlConnection :
    JavaHttpUrlConnectionObject(),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder) {
}
