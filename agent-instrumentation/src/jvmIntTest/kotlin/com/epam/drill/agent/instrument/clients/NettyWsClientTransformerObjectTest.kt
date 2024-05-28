package com.epam.drill.agent.instrument.clients

import java.net.URI
import java.nio.ByteBuffer
import javax.websocket.RemoteEndpoint
import javax.websocket.Session
import org.mockito.Mockito
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.SimpleUserEventChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.netty.handler.codec.http.websocketx.WebSocketVersion

class NettyWsClientTransformerObjectTest : AbstractWsClientTransformerObjectTest() {

    override fun connectToWebsocketAnnotatedEndpoint(endpoint: String): Pair<TestRequestClientEndpoint, Session> =
        connectToWebsocketEndpoint(DefaultProtocolHandlerChannelInitializer(endpoint), endpoint)

    override fun connectToWebsocketInterfaceEndpoint(endpoint: String): Pair<TestRequestClientEndpoint, Session> =
        connectToWebsocketEndpoint(CustomProtocolHandlerChannelInitializer(endpoint), endpoint)

    private fun connectToWebsocketEndpoint(
        initializer: TestChannelInitializer, endpoint: String
    ): Pair<TestRequestClientEndpoint, Session> {
        val uri = URI(endpoint)
        val group = NioEventLoopGroup()
        val channel = Bootstrap()
            .group(group)
            .channel(NioSocketChannel::class.java)
            .handler(initializer)
            .connect(uri.host, uri.port)
            .sync()
            .channel()
        val clientEndpoint = Mockito.mock(TestRequestClientEndpoint::class.java)
        val session = Mockito.mock(Session::class.java)
        val basicRemote = Mockito.mock(RemoteEndpoint.Basic::class.java)
        Mockito.`when`(clientEndpoint.incomingMessages).thenReturn(initializer.incomingMessages)
        Mockito.`when`(session.basicRemote).thenReturn(basicRemote)
        Mockito.`when`(session.close()).then {
            group.shutdownGracefully().sync()
        }
        Mockito.`when`(basicRemote.sendText(Mockito.anyString())).then {
            channel.writeAndFlush(TextWebSocketFrame(it.arguments[0] as String)).sync()
        }
        Mockito.`when`(basicRemote.sendBinary(Mockito.any(ByteBuffer::class.java))).then {
            channel.writeAndFlush(BinaryWebSocketFrame(Unpooled.copiedBuffer(it.arguments[0] as ByteBuffer))).sync()
        }
        initializer.handshakePromise.sync()
        return clientEndpoint to session
    }

    private abstract class TestChannelInitializer(endpoint: String) : ChannelInitializer<SocketChannel>() {
        lateinit var incomingMessages: MutableList<String>
        lateinit var handshakePromise: ChannelPromise
        protected val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            URI(endpoint), WebSocketVersion.V13, null, true, DefaultHttpHeaders()
        )
    }

    private class DefaultProtocolHandlerChannelInitializer(endpoint: String) : TestChannelInitializer(endpoint) {
        override fun initChannel(ch: SocketChannel) {
            incomingMessages = mutableListOf()
            handshakePromise = ch.pipeline().newPromise()
            ch.pipeline().addLast(HttpClientCodec())
            ch.pipeline().addLast(HttpObjectAggregator(2048))
            ch.pipeline().addLast(WebSocketClientProtocolHandler(handshaker))
            ch.pipeline().addLast(HandshakeCompleteEventHandler(handshakePromise))
            ch.pipeline().addLast(TextFrameChannelHandler(incomingMessages))
            ch.pipeline().addLast(BinaryFrameChannelHandler(incomingMessages))
        }
    }

    private class CustomProtocolHandlerChannelInitializer(endpoint: String) : TestChannelInitializer(endpoint) {
        override fun initChannel(ch: SocketChannel) {
            incomingMessages = mutableListOf()
            handshakePromise = ch.pipeline().newPromise()
            ch.pipeline().addLast(HttpClientCodec())
            ch.pipeline().addLast(HttpObjectAggregator(2048))
            ch.pipeline().addLast(HttpWebSocketHandshakeHandler(handshaker, handshakePromise, incomingMessages))
        }
    }

    private class HttpWebSocketHandshakeHandler(
        private val handshaker: WebSocketClientHandshaker,
        private val handshakePromise: ChannelPromise,
        private val incomingMessages: MutableList<String>
    ) : SimpleChannelInboundHandler<FullHttpResponse>() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            super.channelActive(ctx)
            handshaker.handshake(ctx.channel())
        }
        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
            if (!handshaker.isHandshakeComplete) {
                handshaker.finishHandshake(ctx.channel(), msg)
                ctx.pipeline().remove(this)
                ctx.pipeline().addLast(TextFrameChannelHandler(incomingMessages))
                ctx.pipeline().addLast(BinaryFrameChannelHandler(incomingMessages))
                handshakePromise.setSuccess()
            }
        }
    }

    private class HandshakeCompleteEventHandler(
        private val handshakePromise: ChannelPromise
    ) : SimpleUserEventChannelHandler<ClientHandshakeStateEvent>() {
        override fun eventReceived(ctx: ChannelHandlerContext, evt: ClientHandshakeStateEvent) {
            if (evt == ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)
                handshakePromise.setSuccess()
            if (evt == ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT)
                handshakePromise.setFailure(WebSocketHandshakeException(evt.name))
        }
    }

    private class TextFrameChannelHandler(
        private val incomingMessages: MutableList<String>
    ) : SimpleChannelInboundHandler<TextWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            incomingMessages.add(msg.retain().text())
        }
    }

    private class BinaryFrameChannelHandler(
        private val incomingMessages: MutableList<String>
    ) : SimpleChannelInboundHandler<BinaryWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) {
            val message = msg.retain().content()
            incomingMessages.add(ByteArray(message.readableBytes()).also(message::readBytes).decodeToString())
        }
    }

}
