package com.epam.drill.agent.instrument.clients

class ApacheHttpClientTransformerObjectTest : AbstractClientTransformerObjectTest() {

    override fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String>,
        request: String
    ): Pair<Map<String, String>, String> {
        TODO("Not yet implemented")
    }

}
