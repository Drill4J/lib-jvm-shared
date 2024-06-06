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
package com.epam.drill.agent.instrument.clients

import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.servers.NettyTransformerObject

abstract class NettyWsClientTransformerObject : HeadersProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        listOf(
            "io/netty/bootstrap/Bootstrap",
            "io/netty/handler/codec/http/websocketx/WebSocketClientHandshaker"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting NettyWsClientTransformerObject for $className..." }
        when (className) {
            "io/netty/bootstrap/Bootstrap" -> transformBootstrap(ctClass)
            "io/netty/handler/codec/http/websocketx/WebSocketClientHandshaker" -> transformClientHandshaker(ctClass)
        }
    }

    private fun transformBootstrap(ctClass: CtClass) = ctClass
        .getMethod("doResolveAndConnect", "(Ljava/net/SocketAddress;Ljava/net/SocketAddress;)Lio/netty/channel/ChannelFuture;")
        .insertCatching(
            CtBehavior::insertAfter,
            """
            if(${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("${NettyTransformerObject.DRILL_CONTEXT_KEY}");
                ${'$'}_.channel().attr(drillContextKey).set(drillHeaders);
            }
            """.trimIndent()
        )

    private fun transformClientHandshaker(ctClass: CtClass) = ctClass
        .getMethod("handshake", "(Lio/netty/channel/Channel;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("${NettyTransformerObject.DRILL_CONTEXT_KEY}");                                            
            io.netty.util.Attribute drillContextAttr = $1.attr(drillContextKey);
            java.util.Map drillHeaders = (java.util.Map) drillContextAttr.get();
            if (drillHeaders != null) {
                java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                while (iterator.hasNext()) {
                     java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                     if (!customHeaders.contains((String) entry.getKey())) {
                         customHeaders.add((String) entry.getKey(), entry.getValue());
                     }
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(drillHeaders);
            }
            """.trimIndent()
        )

}
