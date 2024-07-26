package com.epam.drill.agent.instrument.netty

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
import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.agent.instrument.TestRequestHolder
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

    override fun connectToWebsocketAnnotatedEndpoint(address: String) =
        connectToWebsocketEndpoint(DefaultClientProtocolHandlerChannelInitializer(address), address)

    override fun connectToWebsocketInterfaceEndpoint(address: String) =
        connectToWebsocketEndpoint(CustomClientProtocolHandlerChannelInitializer(address), address)

    private fun connectToWebsocketEndpoint(
        initializer: TestChannelInitializer,
        endpoint: String
    ): Pair<TestRequestEndpoint, Session> {
        val uri = URI(endpoint)
        val group = NioEventLoopGroup()
        val channel = Bootstrap()
            .group(group)
            .channel(NioSocketChannel::class.java)
            .handler(initializer)
            .connect(uri.host, uri.port)
            .sync()
            .channel()
        val session = Mockito.mock(Session::class.java)
        val basicRemote = Mockito.mock(RemoteEndpoint.Basic::class.java)
        val asyncRemote = Mockito.mock(RemoteEndpoint.Async::class.java)
        Mockito.`when`(session.basicRemote).thenReturn(basicRemote)
        Mockito.`when`(session.asyncRemote).thenReturn(asyncRemote)
        Mockito.`when`(session.close()).then {
            group.shutdownGracefully().sync()
        }
        Mockito.`when`(basicRemote.sendText(Mockito.anyString())).then {
            channel.writeAndFlush(TextWebSocketFrame(it.arguments[0] as String)).sync()
        }
        Mockito.`when`(basicRemote.sendBinary(Mockito.any(ByteBuffer::class.java))).then {
            channel.writeAndFlush(BinaryWebSocketFrame(Unpooled.copiedBuffer(it.arguments[0] as ByteBuffer))).sync()
        }
        Mockito.`when`(asyncRemote.sendText(Mockito.anyString())).then {
            channel.writeAndFlush(TextWebSocketFrame(it.arguments[0] as String))
        }
        Mockito.`when`(asyncRemote.sendBinary(Mockito.any(ByteBuffer::class.java))).then {
            channel.writeAndFlush(BinaryWebSocketFrame(Unpooled.copiedBuffer(it.arguments[0] as ByteBuffer)))
        }
        initializer.handshakePromise.sync()
        return initializer to session
    }

    private class TextFrameServerChannelHandler : SimpleChannelInboundHandler<TextWebSocketFrame>(), TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            val text = msg.retain().text()
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
            val message = msg.retain().content()
            val text = ByteArray(message.readableBytes()).also(message::readBytes).decodeToString()
            processIncoming(text, null)
            TestRequestHolder.store(DrillRequest("$text-response-session", mapOf("drill-data" to "$text-response-data")))
            ctx.writeAndFlush(BinaryWebSocketFrame(Unpooled.copiedBuffer("$text-response".encodeToByteArray())))
            TestRequestHolder.remove()
        }
    }

    private abstract class TestChannelInitializer(
        endpoint: String
    ) : ChannelInitializer<SocketChannel>(), TestRequestEndpoint {
        lateinit var handshakePromise: ChannelPromise
        protected val handshaker: WebSocketClientHandshaker = WebSocketClientHandshakerFactory.newHandshaker(
            URI(endpoint), WebSocketVersion.V13, null, true, DefaultHttpHeaders()
        )
    }

    private class DefaultClientProtocolHandlerChannelInitializer(endpoint: String) : TestChannelInitializer(endpoint) {
        override lateinit var incomingMessages: MutableList<String>
        override lateinit var incomingContexts: MutableList<DrillRequest?>
        override fun initChannel(ch: SocketChannel) {
            incomingMessages = mutableListOf()
            incomingContexts = mutableListOf()
            handshakePromise = ch.pipeline().newPromise()
            ch.pipeline().addLast(HttpClientCodec())
            ch.pipeline().addLast(HttpObjectAggregator(2048))
            ch.pipeline().addLast(WebSocketClientProtocolHandler(handshaker))
            ch.pipeline().addLast(HandshakeCompleteEventHandler(handshakePromise))
            ch.pipeline().addLast(TextFrameClientChannelHandler(incomingMessages, incomingContexts))
            ch.pipeline().addLast(BinaryFrameClientChannelHandler(incomingMessages, incomingContexts))
        }
    }

    private class CustomClientProtocolHandlerChannelInitializer(endpoint: String) : TestChannelInitializer(endpoint) {
        override lateinit var incomingMessages: MutableList<String>
        override lateinit var incomingContexts: MutableList<DrillRequest?>
        override fun initChannel(ch: SocketChannel) {
            incomingMessages = mutableListOf()
            incomingContexts = mutableListOf()
            handshakePromise = ch.pipeline().newPromise()
            ch.pipeline().addLast(HttpClientCodec())
            ch.pipeline().addLast(HttpObjectAggregator(2048))
            ch.pipeline().addLast(
                HttpWebSocketHandshakeHandler(handshaker, handshakePromise, incomingMessages, incomingContexts)
            )
        }
    }

    private class HttpWebSocketHandshakeHandler(
        private val handshaker: WebSocketClientHandshaker,
        private val handshakePromise: ChannelPromise,
        private val incomingMessages: MutableList<String>,
        private val incomingContexts: MutableList<DrillRequest?>
    ) : SimpleChannelInboundHandler<FullHttpResponse>() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            super.channelActive(ctx)
            handshaker.handshake(ctx.channel())
        }
        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
            if (!handshaker.isHandshakeComplete) {
                handshaker.finishHandshake(ctx.channel(), msg)
                ctx.pipeline().remove(this)
                ctx.pipeline().addLast(TextFrameClientChannelHandler(incomingMessages, incomingContexts))
                ctx.pipeline().addLast(BinaryFrameClientChannelHandler(incomingMessages, incomingContexts))
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

    private class TextFrameClientChannelHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : SimpleChannelInboundHandler<TextWebSocketFrame>(), TestRequestEndpoint {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) =
            processIncoming(msg.retain().text(), null)
    }

    private class BinaryFrameClientChannelHandler(
        override val incomingMessages: MutableList<String>,
        override val incomingContexts: MutableList<DrillRequest?>
    ) : SimpleChannelInboundHandler<BinaryWebSocketFrame>(), TestRequestEndpoint {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) = msg.retain().content()
            .let { ByteArray(it.readableBytes()).also(it::readBytes).decodeToString() }
            .let { processIncoming(it, null) }
    }

}
