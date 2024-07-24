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
package com.epam.drill.agent.instrument.netty

import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor

abstract class NettyWsMessagesTransformerObject : HeadersProcessor, PayloadProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        listOf(
            "io/netty/channel/AbstractChannelHandlerContext",
            "io/netty/handler/codec/http/websocketx/WebSocketServerHandshaker",
            "io/netty/handler/codec/http/websocketx/WebSocketClientHandshaker"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting NettyWsMessagesTransformer for $className..." }
        when (className) {
            "io/netty/channel/AbstractChannelHandlerContext" -> transformChannelHandlerContext(ctClass)
            "io/netty/handler/codec/http/websocketx/WebSocketServerHandshaker" -> transformServerHandshaker(ctClass)
            "io/netty/handler/codec/http/websocketx/WebSocketClientHandshaker" -> transformClientHandshaker(ctClass)
        }
    }

    private fun transformChannelHandlerContext(ctClass: CtClass) {
        val invokeChannelReadMethod = ctClass.getMethod("invokeChannelRead", "(Ljava/lang/Object;)V")
        invokeChannelReadMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if (($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT) && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("$DRILL_WS_CONTEXT_KEY");                                            
                io.netty.util.Attribute drillContextAttr = this.channel().attr(drillContextKey);
                java.util.Map drillHeaders = (java.util.Map) drillContextAttr.get();
                if(${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(drillHeaders)) {
                    io.netty.buffer.ByteBuf messageBuf = (($WEBSOCKET_FRAME_COMMON)$1).retain().content();
                    byte[] messageBytes = new byte[messageBuf.readableBytes()];
                    messageBuf.readBytes(messageBytes);
                    messageBytes = ${this::class.java.name}.INSTANCE.retrieveDrillHeaders(messageBytes);
                    $1 = (($WEBSOCKET_FRAME_COMMON)$1).replace(io.netty.buffer.Unpooled.copiedBuffer(messageBytes));
                }
            }
            """.trimIndent()
        )
        invokeChannelReadMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
        ctClass.getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V").insertCatching(
            CtBehavior::insertBefore,
            """
            if (($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT)
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}())) {
                io.netty.buffer.ByteBuf messageBuf = (($WEBSOCKET_FRAME_COMMON)$1).retain().content();
                byte[] messageBytes = new byte[messageBuf.readableBytes()];
                messageBuf.readBytes(messageBytes);
                messageBytes = ${this::class.java.name}.INSTANCE.storeDrillHeaders(messageBytes);
                $1 = (($WEBSOCKET_FRAME_COMMON)$1).replace(io.netty.buffer.Unpooled.copiedBuffer(messageBytes));
            }
            """.trimIndent()
        )
    }

    private fun transformServerHandshaker(ctClass: CtClass) {
        val sendPerMessageHeaderCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                if ($3 == null) $3 = new io.netty.handler.codec.http.DefaultHttpHeaders();
                $3.add("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        ctClass.getMethod(
            "handshake",
            "(Lio/netty/channel/Channel;Lio/netty/handler/codec/http/HttpRequest;Lio/netty/handler/codec/http/HttpHeaders;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;"
        ).insertCatching(CtBehavior::insertBefore, sendPerMessageHeaderCode)
        ctClass.getMethod(
            "handshake",
            "(Lio/netty/channel/Channel;Lio/netty/handler/codec/http/FullHttpRequest;Lio/netty/handler/codec/http/HttpHeaders;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;"
        ).insertCatching(CtBehavior::insertBefore, sendPerMessageHeaderCode)
    }

    private fun transformClientHandshaker(ctClass: CtClass) {
        ctClass.getMethod("handshake","(Lio/netty/channel/Channel;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                    this.customHeaders.add("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
                }
                """.trimIndent()
            )
        ctClass.getMethod("finishHandshake", "(Lio/netty/channel/Channel;Lio/netty/handler/codec/http/FullHttpResponse;)V")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                java.util.Iterator headerNames = $2.headers().names().iterator();
                java.util.Map allHeaders = new java.util.HashMap();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = (String) headerNames.next();
                    java.lang.String headerValue = $2.headers().get(headerName);
                    allHeaders.put(headerName, headerValue);
                }
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("$DRILL_WS_CONTEXT_KEY");
                $1.attr(drillContextKey).set(allHeaders);
                """.trimIndent()
            )
    }

}
