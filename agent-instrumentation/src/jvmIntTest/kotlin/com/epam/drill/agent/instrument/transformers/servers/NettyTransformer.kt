package com.epam.drill.agent.instrument.transformers.servers

import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TestHeadersRetriever
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.instrument.servers.NettyTransformerObject

object NettyTransformer :
    NettyTransformerObject(TestHeadersRetriever),
    HeadersProcessor by DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)