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
import mu.KotlinLogging
import com.epam.drill.agent.instrument.netty.NettyWsTestServer.CustomProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.netty.NettyWsTestServer.DefaultProtocolHandlerChannelInitializer
import com.epam.drill.agent.instrument.servers.AbstractWsServerTransformerObjectTest

class NettyWsServerTransformerObjectTest : AbstractWsServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit) = NettyWsTestServer.withWebSocketServer(
        DefaultProtocolHandlerChannelInitializer(TextFrameChannelHandler(),BinaryFrameChannelHandler()),
        block
    )

    override fun withWebSocketInterfaceEndpoint(block: (String) -> Unit) = NettyWsTestServer.withWebSocketServer(
        CustomProtocolHandlerChannelInitializer(TextFrameChannelHandler(), BinaryFrameChannelHandler()),
        block
    )

    private class TextFrameChannelHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            ctx.writeAndFlush(TextWebSocketFrame(attachSessionHeaders(msg.retain().text())))
        }
    }

    private class BinaryFrameChannelHandler : SimpleChannelInboundHandler<BinaryWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) {
            val message = msg.retain().content()
            val text = ByteArray(message.readableBytes()).also(message::readBytes).decodeToString()
            val response = attachSessionHeaders(text).encodeToByteArray()
            ctx.writeAndFlush(BinaryWebSocketFrame(Unpooled.wrappedBuffer(response)))
        }
    }

}
