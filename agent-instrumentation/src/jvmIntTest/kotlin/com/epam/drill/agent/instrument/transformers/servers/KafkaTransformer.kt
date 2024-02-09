package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TestHeadersRetriever
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.instrument.servers.KafkaTransformerObject

actual object KafkaTransformer :
    TransformerObject,
    KafkaTransformerObject(),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)
