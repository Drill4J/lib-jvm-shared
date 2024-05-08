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
 * Transformer for simple Netty-based web servers
 *
 * Tested with:
 *     io.netty:netty-codec-http:4.1.106.Final
 */
abstract class NettyWsTransformerObject : HeadersProcessor, AbstractTransformerObject() {

    companion object {
        private const val WEBSOCKET_FRAME_TEXT = "io.netty.handler.codec.http.websocketx.TextWebSocketFrame"
        private const val WEBSOCKET_FRAME_BINARY = "io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame"
    }

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        listOf(
            "io/netty/channel/AbstractChannelHandlerContext",
            "io/netty/handler/codec/http/websocketx/WebSocketServerHandshaker"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting NettyWsTransformerObject for $className..." }
        when (className) {
            "io/netty/channel/AbstractChannelHandlerContext" -> transformChannelHandlerContext(ctClass)
            "io/netty/handler/codec/http/websocketx/WebSocketServerHandshaker" -> transformServerHandshaker(ctClass)
        }
    }

    private fun transformChannelHandlerContext(ctClass: CtClass) {
        val invokeChannelReadMethod = ctClass.getMethod("invokeChannelRead", "(Ljava/lang/Object;)V")
        invokeChannelReadMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT) {
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("${NettyTransformerObject.DRILL_CONTEXT_KEY}");                                            
                io.netty.util.Attribute drillContextAttr = this.channel().attr(drillContextKey);
                java.util.Map drillHeaders = (java.util.Map) drillContextAttr.get();
                if (drillHeaders != null) {
                    ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
                }
            }
            """.trimIndent()
        )
        invokeChannelReadMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
        val writeMethod = ctClass.getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V")
        writeMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if($1 instanceof $WEBSOCKET_FRAME_BINARY || ${'$'}1 instanceof $WEBSOCKET_FRAME_TEXT) {
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("${NettyTransformerObject.DRILL_CONTEXT_KEY}");                                            
                io.netty.util.Attribute drillContextAttr = this.channel().attr(drillContextKey);
                java.util.Map drillHeaders = (java.util.Map) drillContextAttr.get();
                if (drillHeaders != null) {
                    ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
                }
            }
            """.trimIndent()
        )
        writeMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformServerHandshaker(ctClass: CtClass) {
        val storeHandshakerCode =
            """
            if(io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.getHandshaker($1) == null) {
                io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.setHandshaker($1, this);
            }
            """.trimIndent()
        ctClass.getMethod(
            "handshake",
            "(Lio/netty/channel/Channel;Lio/netty/handler/codec/http/HttpRequest;Lio/netty/handler/codec/http/HttpHeaders;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;"
        ).insertCatching(CtBehavior::insertBefore, storeHandshakerCode)
        ctClass.getMethod(
            "handshake",
            "(Lio/netty/channel/Channel;Lio/netty/handler/codec/http/FullHttpRequest;Lio/netty/handler/codec/http/HttpHeaders;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;"
        ).insertCatching(CtBehavior::insertBefore, storeHandshakerCode)
    }

}
