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
            if(($1 instanceof $WEBSOCKET_FRAME_BINARY || $1 instanceof $WEBSOCKET_FRAME_TEXT) && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                
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
    }

    private fun transformServerHandshaker(ctClass: CtClass) {
        val sendPerMessageHeaderCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
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

    private fun transformClientHandshaker(ctClass: CtClass) = ctClass
        .getMethod("handshake", "(Lio/netty/channel/Channel;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                this.customHeaders.add("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )

}
