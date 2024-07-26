package com.epam.drill.agent.instrument.netty

import java.net.InetSocketAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler

object NettyWsTestServer {

    fun withWebSocketServer(initializer: ChannelInitializer<SocketChannel>, block: (String) -> Unit) {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        lateinit var serverChannel: Channel
        try {
            serverChannel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(initializer)
                .bind(InetSocketAddress(0))
                .sync()
                .channel()
            val address = serverChannel.localAddress() as InetSocketAddress
            block("ws://localhost:${address.port}")
        } finally {
            serverChannel.close().sync()
            workerGroup.shutdownGracefully().sync()
            bossGroup.shutdownGracefully().sync()
        }
    }

    class DefaultProtocolHandlerChannelInitializer(
        private val textMessageHandler: SimpleChannelInboundHandler<TextWebSocketFrame>,
        private val binaryMessageHandler: SimpleChannelInboundHandler<BinaryWebSocketFrame>
    ) : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec())
            ch.pipeline().addLast(WebSocketServerProtocolHandler("/"))
            ch.pipeline().addLast(textMessageHandler)
            ch.pipeline().addLast(binaryMessageHandler)
        }
    }

    class CustomProtocolHandlerChannelInitializer(
        private val textMessageHandler: SimpleChannelInboundHandler<TextWebSocketFrame>,
        private val binaryMessageHandler: SimpleChannelInboundHandler<BinaryWebSocketFrame>
    ) : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec())
            ch.pipeline().addLast(HttpWebSocketHandshakeHandler(textMessageHandler, binaryMessageHandler))
        }
    }

    private class HttpWebSocketHandshakeHandler(
        private val textMessageHandler: SimpleChannelInboundHandler<TextWebSocketFrame>,
        private val binaryMessageHandler: SimpleChannelInboundHandler<BinaryWebSocketFrame>
    ) : SimpleChannelInboundHandler<HttpRequest>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
            if (HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(msg.headers().get(HttpHeaderNames.CONNECTION)) &&
                HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(msg.headers().get(HttpHeaderNames.UPGRADE))) {
                ctx.pipeline().remove(this)
                ctx.pipeline().addLast(textMessageHandler)
                ctx.pipeline().addLast(binaryMessageHandler)
                val wsUrl = "ws://${msg.headers().get(HttpHeaderNames.HOST)}${msg.uri()}"
                WebSocketServerHandshakerFactory(wsUrl, null, true).newHandshaker(msg)
                    ?.handshake(ctx.channel(), msg)
                    ?: WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
            }
        }
    }

}
