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

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.epam.drill.agent.instrument.clients.AbstractWsClientTransformerObjectTest
import com.epam.drill.agent.instrument.netty.NettyWsTestClient.CustomClientProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.netty.NettyWsTestClient.DefaultClientProtocolHandlerChannelInitializer

class NettyWsClientTransformerObjectTest : AbstractWsClientTransformerObjectTest() {

    override fun connectToWebsocketAnnotatedEndpoint(endpoint: String) = TestRequestClientEndpointImpl().run {
        val session = NettyWsTestClient.connectToWebsocketEndpoint(
            endpoint,
            DefaultClientProtocolHandlerChannelInitializer(
                endpoint,
                TextFrameChannelHandler(this.incomingMessages),
                BinaryFrameChannelHandler(this.incomingMessages)
            )
        )
        this to session
    }

    override fun connectToWebsocketInterfaceEndpoint(endpoint: String) = TestRequestClientEndpointImpl().run {
        val session = NettyWsTestClient.connectToWebsocketEndpoint(
            endpoint,
            CustomClientProtocolHandlerChannelInitializer(
                endpoint,
                TextFrameChannelHandler(this.incomingMessages),
                BinaryFrameChannelHandler(this.incomingMessages)
            )
        )
        this to session
    }

    class TestRequestClientEndpointImpl : TestRequestClientEndpoint {
        override val incomingMessages = mutableListOf<String>()
    }

    private class TextFrameChannelHandler(
        private val incomingMessages: MutableList<String>
    ) : SimpleChannelInboundHandler<TextWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            incomingMessages.add(msg.text())
        }
    }

    private class BinaryFrameChannelHandler(
        private val incomingMessages: MutableList<String>
    ) : SimpleChannelInboundHandler<BinaryWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) {
            val message = msg.content()
            incomingMessages.add(ByteArray(message.readableBytes()).also(message::readBytes).decodeToString())
        }
    }

}
