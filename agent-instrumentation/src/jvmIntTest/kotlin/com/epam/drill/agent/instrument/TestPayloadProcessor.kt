package com.epam.drill.agent.instrument

object TestPayloadProcessor : DrillRequestPayloadProcessor(headersProcessor = TestHeadersProcessor) {

    var enabled = true

    override fun isPayloadProcessingEnabled() = enabled

}
