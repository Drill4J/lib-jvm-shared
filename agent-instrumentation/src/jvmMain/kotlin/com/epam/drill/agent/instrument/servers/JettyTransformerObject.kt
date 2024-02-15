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
        "org/eclipse/jetty/servlet/ServletHolder" == className

    override fun transform(className: String, ctClass: CtClass) {
        val adminHeader = headersRetriever.adminAddressHeader()
        val adminUrl = headersRetriever.adminAddressValue()
        val agentIdHeader = headersRetriever.agentIdHeader()
        val agentIdValue = headersRetriever.agentIdHeaderValue()
        logger.info { "transform: Starting JettyTransformer with admin host $adminUrl..." }

        val method =
            ctClass.getMethod(
                "handle",
                "(Lorg/eclipse/jetty/server/Request;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"
            )

        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1 instanceof org.eclipse.jetty.server.Request && $2 instanceof org.eclipse.jetty.server.Request && $3 instanceof org.eclipse.jetty.server.Response) {
                org.eclipse.jetty.server.Response jettyResponse = (org.eclipse.jetty.server.Response)$3;
                if (!"$adminUrl".equals(jettyResponse.getHeader("$adminHeader"))) {
                    jettyResponse.addHeader("$adminHeader", "$adminUrl");
                    jettyResponse.addHeader("$agentIdHeader", "$agentIdValue");
                }
                org.eclipse.jetty.server.Request jettyRequest = (org.eclipse.jetty.server.Request)$2;
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
            CtBehavior::insertAfter,
            """
            ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            """.trimIndent()
        )
    }
}
