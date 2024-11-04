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

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.agent.instrument.netty.NettyWsTestClient.CustomClientProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.netty.NettyWsTestClient.DefaultClientProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.netty.NettyWsTestServer.CustomProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.netty.NettyWsTestServer.DefaultProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.servers.AbstractWsMessagesTransformerObjectTest

class NettyWsMessagesTransformerObjectTest : AbstractWsMessagesTransformerObjectTest() {

    override fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit) = NettyWsTestServer.withWebSocketServer(
        DefaultProtocolHandlerChannelInitializer(TextFrameServerChannelHandler(), BinaryFrameServerChannelHandler()),
        block
    )

    override fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit) = NettyWsTestServer.withWebSocketServer(
        CustomProtocolHandlerChannelInitializer(TextFrameServerChannelHandler(), BinaryFrameServerChannelHandler()),
        block
    )

    override fun connectToWebsocketAnnotatedEndpoint(address: String) = TestRequestEndpointImpl().run {
        val session = NettyWsTestClient.connectToWebsocketEndpoint(
            address,
            DefaultClientProtocolHandlerChannelInitializer(
                address,
                TextFrameClientChannelHandler(this.incomingMessages, this.incomingContexts),
                BinaryFrameClientChannelHandler(this.incomingMessages, this.incomingContexts)
            )
        )
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(address: String) = TestRequestEndpointImpl().run {
        val session = NettyWsTestClient.connectToWebsocketEndpoint(
            address,
            CustomClientProtocolHandlerChannelInitializer(
                address,
                TextFrameClientChannelHandler(this.incomingMessages, this.incomingContexts),
                BinaryFrameClientChannelHandler(this.incomingMessages, this.incomingContexts)
            )
        )
        this to session
    }

    class TestRequestEndpointImpl : TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
    }

    private class TextFrameServerChannelHandler : SimpleChannelInboundHandler<TextWebSocketFrame>(), TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            val text = msg.text()
            processIncoming(text, null)
            TestRequestHolder.store(DrillRequest("$text-response-session", mapOf("drill-data" to "$text-response-data")))
            ctx.writeAndFlush(TextWebSocketFrame("$text-response"))
            TestRequestHolder.remove()
        }
    }

    private class BinaryFrameServerChannelHandler : SimpleChannelInboundHandler<BinaryWebSocketFrame>(), TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) {
            val message = msg.content()
            val text = ByteArray(message.readableBytes()).also(message::readBytes).decodeToString()
            processIncoming(text, null)
            TestRequestHolder.store(DrillRequest("$text-response-session", mapOf("drill-data" to "$text-response-data")))
            ctx.writeAndFlush(BinaryWebSocketFrame(Unpooled.wrappedBuffer("$text-response".encodeToByteArray())))
            TestRequestHolder.remove()
        }
    }

    private class TextFrameClientChannelHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : SimpleChannelInboundHandler<TextWebSocketFrame>(), TestRequestEndpoint {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) =
            processIncoming(msg.text(), null)
    }

    private class BinaryFrameClientChannelHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : SimpleChannelInboundHandler<BinaryWebSocketFrame>(), TestRequestEndpoint {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) = msg.content()
            .let { ByteArray(it.readableBytes()).also(it::readBytes).decodeToString() }
            .let { processIncoming(it, null) }
    }

}
