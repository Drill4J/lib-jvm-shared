/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.common.agent.request.HeadersRetriever
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

/**
 * Transformer for simple Undertow-based web servers
 *
 * Tested with:
 *      io.undertow:undertow-core:2.0.29.Final
 */
abstract class UndertowTransformerObject(
    protected val headersRetriever: HeadersRetriever
) : HeadersProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        "io/undertow/server/Connectors" == className

    override fun transform(className: String, ctClass: CtClass) {
        val adminHeader = headersRetriever.adminAddressHeader()
        val adminUrl = headersRetriever.adminAddressValue()
        val agentIdHeader = headersRetriever.agentIdHeader()
        val agentIdValue = headersRetriever.agentIdHeaderValue()

        logger.info { "transform: Starting UndertowTransformer with admin host $adminUrl..." }
        val method = ctClass.getMethod("executeRootHandler", "(Lio/undertow/server/HttpHandler;Lio/undertow/server/HttpServerExchange;)V")

        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1 instanceof io.undertow.server.HttpHandler && $2 instanceof io.undertow.server.HttpServerExchange) {
                io.undertow.util.HeaderMap responseHeaders = (io.undertow.util.HeaderMap) $2.getResponseHeaders();
                if (!"$adminUrl".equals(responseHeaders.get("$adminHeader"))) {
                    responseHeaders.add(io.undertow.util.HttpString.tryFromString("$adminHeader"), "$adminUrl");
                    responseHeaders.add(io.undertow.util.HttpString.tryFromString("$agentIdHeader"), "$agentIdValue");
                }
        
                io.undertow.util.HeaderMap requestHeaders = (io.undertow.util.HeaderMap) $2.getRequestHeaders();
                java.util.Iterator/*io.undertow.util.HttpString>*/ headerNames = requestHeaders.getHeaderNames().iterator();
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                while (headerNames.hasNext()){
                    java.lang.String headerName = (java.lang.String) headerNames.next().toString();
        
                    java.lang.String header = requestHeaders.get(io.undertow.util.HttpString.tryFromString(headerName)).toString();
                    allHeaders.put(headerName, header);
        
                    if (headerName.startsWith("${HeadersProcessor.DRILL_HEADER_PREFIX}") && responseHeaders.get(io.undertow.util.HttpString.tryFromString(headerName)) == null) {
                        responseHeaders.add(io.undertow.util.HttpString.tryFromString(headerName), header);
                    }
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
        method.insertCatching(
            CtBehavior::insertAfter,
            """
            ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            """.trimIndent()
        )
    }

}
