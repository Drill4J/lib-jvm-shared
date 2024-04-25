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

/**
 * Transformer for Jetty based websockets
 *
 * Tested with:
 *     org.eclipse.jetty.websocket:javax-websocket-server-impl:9.4.26.v20200117
 */
abstract class JettyWsTransformerObject : HeadersProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        "org/eclipse/jetty/websocket/common/events/AbstractEventDriver" == className

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting JettyWsTransformerObject..." }
        val method = ctClass.getMethod("incomingFrame", "(Lorg/eclipse/jetty/websocket/api/extensions/Frame;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = this.session.getUpgradeRequest().getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = this.session.getUpgradeRequest().getHeaders().get(headerName);
                    java.lang.String header = java.lang.String.join(",", headerValues);
                    allHeaders.put(headerName, header);
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
        method.insertCatching(
            CtBehavior::insertAfter,
            """
            if (($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY) && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

}
