package com.epam.drill.agent.instrument.netty

const val HTTP_REQUEST = "io.netty.handler.codec.http.HttpRequest"
const val HTTP_RESPONSE = "io.netty.handler.codec.http.HttpResponse"
const val WEBSOCKET_FRAME_TEXT = "io.netty.handler.codec.http.websocketx.TextWebSocketFrame"
const val WEBSOCKET_FRAME_BINARY = "io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame"
const val DRILL_HTTP_CONTEXT_KEY = "com.epam.drill.common.agent.request.DrillRequest#DRILL_REQUEST_HTTP"
const val DRILL_WS_CONTEXT_KEY = "com.epam.drill.common.agent.request.DrillRequest#DRILL_REQUEST_WS"
const val WEB_SOCKET_SERVER_HANDSHAKER_KEY = "io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker#HANDSHAKER"
