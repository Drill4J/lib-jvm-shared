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


import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.common.agent.request.HeadersRetriever

abstract class JettyTransformerObject(
    protected val headersRetriever: HeadersRetriever
) : HeadersProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        "org/eclipse/jetty/server/handler/HandlerWrapper" == className

    override fun transform(className: String, ctClass: CtClass) {
        val adminHeader = headersRetriever.adminAddressHeader()
        val adminUrl = headersRetriever.adminAddressValue()
        val agentIdHeader = headersRetriever.agentIdHeader()
        val agentIdValue = headersRetriever.agentIdHeaderValue()
        logger.info { "transform: Starting JettyTransformer with admin host $adminUrl..." }
        val method =
            ctClass.getMethod(
                "handle",
                "(Ljava/lang/String;Lorg/eclipse/jetty/server/Request;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"
            )
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($2 instanceof org.eclipse.jetty.server.Request && $3 instanceof org.eclipse.jetty.server.Request && $4 instanceof org.eclipse.jetty.server.Response) {
                org.eclipse.jetty.server.Response jettyResponse = (org.eclipse.jetty.server.Response)$4;
                if (!"$adminUrl".equals(jettyResponse.getHeader("$adminHeader"))) {
                    jettyResponse.addHeader("$adminHeader", "$adminUrl");
                    jettyResponse.addHeader("$agentIdHeader", "$agentIdValue");
                }
                org.eclipse.jetty.server.Request jettyRequest = (org.eclipse.jetty.server.Request)$3;
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Enumeration/*<String>*/ headerNames = jettyRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    java.lang.String headerName = (java.lang.String) headerNames.nextElement();
                    java.lang.String header = jettyRequest.getHeader(headerName);
                    allHeaders.put(headerName, header);
                    if (headerName.startsWith("${HeadersProcessor.DRILL_HEADER_PREFIX}") && jettyResponse.getHeader(headerName) == null) {
                        jettyResponse.addHeader(headerName, header);
                    }
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
        method.insertCatching(
            { insertAfter(it, true) },
            """
            ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            """.trimIndent()
        )
    }
}
