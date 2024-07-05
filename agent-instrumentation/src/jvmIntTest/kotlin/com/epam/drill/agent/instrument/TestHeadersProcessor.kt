package com.epam.drill.agent.instrument

object TestHeadersProcessor : DrillRequestHeadersProcessor(TestHeadersRetriever, TestRequestHolder)
