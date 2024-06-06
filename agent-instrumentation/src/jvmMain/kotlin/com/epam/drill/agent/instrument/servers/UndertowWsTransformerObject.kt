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
import javassist.CtField
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor

/**
 * Transformer for Undertow-based websockets
 *
 * Tested with:
 *      io.undertow:undertow-websockets-jsr:2.0.29.Final
 */
abstract class UndertowWsTransformerObject : HeadersProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}
    private var openingSession: ThreadLocal<Map<String, String>?> = ThreadLocal()

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        listOf(
            "io/undertow/websockets/jsr/UndertowSession",
            "io/undertow/websockets/jsr/EndpointSessionHandler",
            "io/undertow/websockets/jsr/FrameHandler"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting UndertowWsTransformer for $className..." }
        when (className) {
            "io/undertow/websockets/jsr/UndertowSession" -> transformSession(ctClass)
            "io/undertow/websockets/jsr/EndpointSessionHandler" -> transformSessionHandler(ctClass)
            "io/undertow/websockets/jsr/FrameHandler" -> transformFrameHandler(ctClass)
        }
    }

    fun setHandshakeHeaders(session: Map<String, String>?) = openingSession.set(session)

    fun getHandshakeHeaders() = openingSession.get()

    private fun transformSession(ctClass: CtClass) {
        CtField.make(
            "private java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders = null;",
            ctClass
        ).also(ctClass::addField)
        CtMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.lang.String>*/ getHandshakeHeaders() {
                return this.handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        ctClass.constructors[0].insertCatching(
            CtBehavior::insertAfter,
            """
            this.handshakeHeaders = ${this::class.java.name}.INSTANCE.${this::getHandshakeHeaders.name}();
            ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(null);
            """.trimIndent()
        )
    }

    private fun transformSessionHandler(ctClass: CtClass) {
        val method = ctClass.getMethod("onConnect", "(Lio/undertow/websockets/spi/WebSocketHttpExchange;Lio/undertow/websockets/core/WebSocketChannel;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = $1.getRequestHeaders().keySet().iterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = headerNames.next();
                java.util.List/*<java.lang.String>*/ headerValues = $1.getRequestHeaders().get(headerName);
                java.lang.String header = java.lang.String.join(",", headerValues);
                allHeaders.put(headerName, header);
            }
            ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(allHeaders);
            """.trimIndent()
        )
    }

    private fun transformFrameHandler(ctClass: CtClass) {
        val onTextMethod = ctClass.getMethod("onText", "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/StreamSourceFrameChannel;)V")
        val onBinaryMethod = ctClass.getMethod("onBinary", "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/StreamSourceFrameChannel;)V")
        val storeHeadersCode =
            """
            if (this.session.getHandshakeHeaders() != null) {
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(this.session.getHandshakeHeaders());
            }
            """.trimIndent()
        val removeHeadersCode =
            """
            if (this.session.getHandshakeHeaders() != null) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        onTextMethod.insertCatching(CtBehavior::insertBefore, storeHeadersCode)
        onTextMethod.insertCatching(CtBehavior::insertAfter, removeHeadersCode)
        onBinaryMethod.insertCatching(CtBehavior::insertBefore, storeHeadersCode)
        onBinaryMethod.insertCatching(CtBehavior::insertAfter, removeHeadersCode)
    }

}