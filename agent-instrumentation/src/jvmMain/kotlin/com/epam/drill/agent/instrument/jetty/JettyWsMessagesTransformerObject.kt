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
package com.epam.drill.agent.instrument.jetty

import javassist.CtBehavior
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor

abstract class JettyWsMessagesTransformerObject : HeadersProcessor, PayloadProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        listOf(
            "org/eclipse/jetty/websocket/common/WebSocketSession",
            "org/eclipse/jetty/websocket/common/events/AbstractEventDriver",
            "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler",
            "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler",
            "org/eclipse/jetty/websocket/common/WebSocketRemoteEndpoint",
            "org/eclipse/jetty/websocket/client/WebSocketClient",
            "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient",
            "org/eclipse/jetty/websocket/server/HandshakeRFC6455"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting JettyWsMessagesTransformerObject for $className..." }
        when (className) {
            "org/eclipse/jetty/websocket/common/WebSocketSession" -> transformWebSocketSession(ctClass)
            "org/eclipse/jetty/websocket/common/events/AbstractEventDriver" -> transformAbstractEventDriver(ctClass)
            "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler" -> transformJettyWebSocketFrameHandler(ctClass)
            "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler" -> transformJavaxWebSocketFrameHandler(ctClass)
            "org/eclipse/jetty/websocket/common/WebSocketRemoteEndpoint" -> transformRemoteEndpoint(ctClass)
            "org/eclipse/jetty/websocket/client/WebSocketClient" -> transformWebSocketClient(ctClass)
            "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient" -> transformWebSocketCoreClient(ctClass)
            "org/eclipse.jetty/websocket/server/HandshakeRFC6455" -> transformHandshakeRFC(ctClass)
        }
    }

    private fun transformWebSocketSession(ctClass: CtClass) {
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
        ctClass.getMethod("setUpgradeRequest", "(Lorg/eclipse/jetty/websocket/api/UpgradeRequest;)V").insertCatching(
            CtBehavior::insertAfter,
            """
            if($1 instanceof org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest) {
                this.handshakeHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = $1.getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = $1.getHeaders().get(headerName);
                    java.lang.String header = java.lang.String.join(",", headerValues);
                    this.handshakeHeaders.put(headerName, header);
                }
            }
            """.trimIndent()
        )
        ctClass.getMethod("setUpgradeResponse", "(Lorg/eclipse/jetty/websocket/api/UpgradeResponse;)V").insertCatching(
            CtBehavior::insertAfter,
            """
            if($1 instanceof org.eclipse.jetty.websocket.client.ClientUpgradeResponse) {
                this.handshakeHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = $1.getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = $1.getHeaders().get(headerName);
                    java.lang.String header = java.lang.String.join(",", headerValues);
                    this.handshakeHeaders.put(headerName, header);
                }
            }
            """.trimIndent()
        )
    }

    private fun transformAbstractEventDriver(ctClass: CtClass) {
        val method = ctClass.getMethod("incomingFrame", "(Lorg/eclipse/jetty/websocket/api/extensions/Frame;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if (($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY)
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeHeaders())) {
                byte[] bytes = new byte[$1.getPayload().limit()];
                int position = $1.getPayload().position();                
                $1.getPayload().get(bytes);
                $1.getPayload().position(position);
                java.lang.Integer drillIndex = ${this::class.java.name}.INSTANCE.${this::retrieveDrillHeadersIndex.name}(bytes);
                if (drillIndex != null) {
                    $1.getPayload().limit(drillIndex.intValue());
                }
            }
            """.trimIndent()
        )
        method.insertCatching(
            { insertAfter(it, true) },
            """
            if (($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY)
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeHeaders())) {
                 ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformJettyWebSocketFrameHandler(ctClass: CtClass) {
    }

    private fun transformJavaxWebSocketFrameHandler(ctClass: CtClass) {
    }

    private fun transformRemoteEndpoint(ctClass: CtClass) {
        //Basic.sendBinary(ByteBuffer data)                         -> sendBytes(data)
        //Basic.sendBinary(ByteBuffer partialByte, boolean isLast)  -> sendPartialBytes(partialByte, isLast)
        //Basic.sendObject(Object data)                             -> sendStringByFuture(msg)
        //                                                          -> sendBytesByFuture(buf)
        //Basic.sendText(String text)                               -> sendString(text)
        //Basic.sendText(String partialMessage, boolean isLast)     -> sendPartialString(partialMessage, isLast)
        //Async.sendBinary(ByteBuffer data)                         -> sendBytesByFuture(data)
        //Async.sendBinary(ByteBuffer data, SendHandler handler)    -> uncheckedSendFrame(frame, new SendHandlerWriteCallback(handler))
        //Async.sendObject(Object data)                             -> sendStringByFuture(msg)
        //                                                          -> sendBytesByFuture(buf)
        //Async.sendObject(Object data, SendHandler handler)        ->
        //Async.sendText(String text)                               -> sendStringByFuture(text)
        //Async.sendText(String text, SendHandler handler)          -> uncheckedSendFrame(frame, new SendHandlerWriteCallback(handler))
        //-----------------------------------------------------------------------------------------------------------------------------
        //sendBytes(data)                                                   -> blockingWrite(new BinaryFrame().setPayload(data))
        //sendPartialBytes(partialByte, isLast)                             -> blockingWrite(frame)
        //sendBytesByFuture(buf)                                            -> sendAsyncFrame(new BinaryFrame().setPayload(data))
        //sendString(text)                                                  -> blockingWrite(frame)
        //sendPartialString(partialMessage, isLast)                         -> blockingWrite(frame)
        //sendStringByFuture(msg)                                           -> sendAsyncFrame(frame)
        //uncheckedSendFrame(frame, new SendHandlerWriteCallback(handler))  -> outgoing.outgoingFrame(frame, callback, batchMode)
        val wrapStringCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(((org.eclipse.jetty.websocket.common.WebSocketSession)this.outgoing).getHandshakeHeaders())) {
                $1 = ${this::class.java.name}.INSTANCE.storeDrillHeaders($1);
            }
            """.trimIndent()
        val wrapBinaryCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(((org.eclipse.jetty.websocket.common.WebSocketSession)this.outgoing).getHandshakeHeaders())) {
                byte[] bytes = new byte[$1.limit()];
                $1.get(bytes);
                $1.clear();
                $1 = java.nio.ByteBuffer.wrap(${this::class.java.name}.INSTANCE.storeDrillHeaders(bytes));
            }
            """.trimIndent()
        ctClass.getMethod("sendString", "(Ljava/lang/String;)V")
            .insertCatching(CtBehavior::insertBefore, wrapStringCode)
        ctClass.getMethod("sendPartialString", "(Ljava/lang/String;Z)V")
            .insertCatching(CtBehavior::insertBefore, wrapStringCode)
        ctClass.getMethod("sendStringByFuture", "(Ljava/lang/String;)Ljava/util/concurrent/Future;")
            .insertCatching(CtBehavior::insertBefore, wrapStringCode)
        ctClass.getMethod("sendBytes", "(Ljava/nio/ByteBuffer;)V")
            .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        ctClass.getMethod("sendPartialBytes", "(Ljava/nio/ByteBuffer;Z)V")
            .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        ctClass.getMethod("sendBytesByFuture", "(Ljava/nio/ByteBuffer;)Ljava/util/concurrent/Future;")
            .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        ctClass.getMethod("uncheckedSendFrame", "(Lorg/eclipse/jetty/websocket/common/WebSocketFrame;Lorg/eclipse/jetty/websocket/api/WriteCallback;)V")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                        && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                        && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(((org.eclipse.jetty.websocket.common.WebSocketSession)this.outgoing).getHandshakeHeaders())) {
                    byte[] bytes = new byte[$1.getPayload().limit()];
                    $1.getPayload().get(bytes);
                    $1.getPayload().clear();
                    $1.setPayload(java.nio.ByteBuffer.wrap(${this::class.java.name}.INSTANCE.storeDrillHeaders(bytes)));
                }
                """.trimIndent()
            )
    }

    private fun transformWebSocketClient(ctClass: CtClass) = ctClass
        .getMethod("connect", "(Ljava/lang/Object;Ljava/net/URI;Lorg/eclipse/jetty/websocket/client/ClientUpgradeRequest;Lorg/eclipse/jetty/websocket/client/io/UpgradeListener;)Ljava/util/concurrent/Future;")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $3.setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )

    private fun transformWebSocketCoreClient(ctClass: CtClass) = ctClass
        .getMethod("connect", "(Lorg/eclipse/jetty/websocket/core/client/CoreClientUpgradeRequest;)Ljava/util/concurrent/CompletableFuture;")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $1.addHeader(new org.eclipse.jetty.http.HttpField("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true"));
            }
            """.trimIndent()
        )

    private fun transformHandshakeRFC(ctClass: CtClass) = ctClass
        .getMethod("doHandshakeResponse", "(Lorg/eclipse/jetty/websocket/servlet/ServletUpgradeRequest;Lorg/eclipse/jetty/websocket/servlet/ServletUpgradeResponse;)V")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $2.setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )

}
