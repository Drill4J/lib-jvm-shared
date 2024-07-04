package com.epam.drill.agent.instrument.transformers.undertow

import com.epam.drill.agent.instrument.ClassPathProvider
import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.DrillRequestPayloadProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor
import com.epam.drill.agent.instrument.TestClassPathProvider
import com.epam.drill.agent.instrument.TestHeadersRetriever
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.instrument.undertow.UndertowWsMessagesTransformerObject

actual object UndertowWsMessagesTransformer:
    TransformerObject,
    UndertowWsMessagesTransformerObject(),
    HeadersProcessor by headersProcessor,
    PayloadProcessor by DrillRequestPayloadProcessor(true, headersProcessor),
    ClassPathProvider by TestClassPathProvider

private val headersProcessor = DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)
