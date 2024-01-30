package com.epam.drill.agent.instrument.clients

import com.epam.drill.agent.instrument.AbstractClientTransformerObjectTest

class ApacheHttpClientConnectionObjectTest : AbstractClientTransformerObjectTest() {

    override fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String>,
        request: String
    ): Pair<Map<String, String>, String> {
        TODO("Not yet implemented")
    }

}
