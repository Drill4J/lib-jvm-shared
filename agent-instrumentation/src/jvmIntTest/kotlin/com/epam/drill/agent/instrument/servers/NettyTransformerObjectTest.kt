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

import java.net.InetSocketAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import mu.KotlinLogging

class NettyTransformerObjectTest : AbstractServerTransformerObjectTest() {

    override val logger = KotlinLogging.logger {}

    override fun withHttpServer(block: (String) -> Unit) {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        lateinit var serverChannel: Channel
        try {
            serverChannel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(TestRequestChannelInitializer())
                .bind(InetSocketAddress(0))
                .sync()
                .channel()
            val address = serverChannel.localAddress() as InetSocketAddress
            block("http://localhost:${address.port}")
        } finally {
            serverChannel.close().sync()
            workerGroup.shutdownGracefully().sync()
            bossGroup.shutdownGracefully().sync()
        }
    }

    private class TestRequestChannelInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec())
            ch.pipeline().addLast(TestRequestChannelHandler())
        }
    }

    private class TestRequestChannelHandler : SimpleChannelInboundHandler<LastHttpContent>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: LastHttpContent) {
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, msg.retain().content())
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        }
    }

}
