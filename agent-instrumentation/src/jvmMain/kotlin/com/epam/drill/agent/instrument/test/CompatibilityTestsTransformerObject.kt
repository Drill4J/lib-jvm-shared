package com.epam.drill.agent.instrument.test

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.common.agent.request.RequestHolder
import javassist.CtClass
import mu.KotlinLogging

private const val COMPATIBILITY_TEST_CLASS_NAME = "com/epam/test/drill/DrillTestContext"
private const val DRILL_SESSION_ID_HEADER = "drill-session-id"

/**
 * Uses for compatibility tests https://github.com/Drill4J/internal-compatibility-matrix-tests.
 */
abstract class CompatibilityTestsTransformerObject :
    TransformerObject,
    RequestHolder,
    AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == COMPATIBILITY_TEST_CLASS_NAME

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getDeclaredMethod("retrieve").setBody(
            """             
                {
                    ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                    if (drillRequest != null) {
                        java.util.Map context = new java.util.HashMap();
                        context.putAll(drillRequest.getHeaders());
                        context.put("$DRILL_SESSION_ID_HEADER", drillRequest.getDrillSessionId());
                        return context;
                    } else {
                        return null;
                    }                                            
                }            
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("store").setBody(
            """
                {
                    java.lang.String sessionId = (java.lang.String) $1.get("$DRILL_SESSION_ID_HEADER");
                    ${DrillRequest::class.java.name} drillRequest = new ${DrillRequest::class.java.name}(sessionId, $1);
                    ${this::class.java.name}.INSTANCE.${RequestHolder::store.name}(drillRequest);                
                }                    
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("remove").setBody(
            """    
                {                            
                    ${this::class.java.name}.INSTANCE.${RequestHolder::remove.name}();
                }                    
            """.trimIndent()
        )
    }

}
