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

import kotlin.reflect.KCallable
import java.lang.reflect.Method
import java.nio.ByteBuffer
import javassist.ByteArrayClassPath
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import mu.KotlinLogging
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.FieldValue
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.matcher.ElementMatchers
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor

/**
 * Transformer for Undertow-based websockets
 *
 * Tested with:
 *      io.undertow:undertow-websockets-jsr:2.0.29.Final
 */
abstract class UndertowWsTransformerObject : HeadersProcessor, PayloadProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}
    private val byteBuddy by lazy(::ByteBuddy)
    private val proxyClassCache = TypeCache<String>()
    private var openingSession: ThreadLocal<Map<String, String>?> = ThreadLocal()
    private lateinit var pooledProxyClass: Class<*>

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        listOf(
            "io/undertow/websockets/jsr/UndertowSession",
            "io/undertow/websockets/jsr/EndpointSessionHandler",
            "io/undertow/websockets/jsr/FrameHandler"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting UndertowWsTransformer for $className..." }
        when (className) {
            "io/undertow/websockets/jsr/UndertowSession" -> transformSession(ctClass)
            "io/undertow/websockets/jsr/EndpointSessionHandler" -> transformSessionHandler(ctClass)
            "io/undertow/websockets/jsr/FrameHandler" -> transformFrameHandler(ctClass)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHandshakeHeaders(session: Map<String, String>?) = openingSession.set(session)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getHandshakeHeaders() = openingSession.get()

    @RuntimeType
    fun delegatedTextMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as String)
        .also { logger.trace { "delegatedTextMessageData: Payload received: $it" } }
        .let(::retrievePayload)
        .also { storeHeaders(it.second) }
        .let(Pair<String, Map<String, String>>::first)

    @RuntimeType
    fun delegatedBinaryMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = method.invoke(target).let { pooledProxyClass.constructors[0].newInstance(it)!! }

    @RuntimeType
    @Suppress("unchecked_cast")
    fun delegatedPooledResource(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as Array<ByteBuffer>).let {
        val simpleArray = it.size == 1 && it[0].hasArray()
                && it[0].arrayOffset() == 0 && it[0].position() == 0 && it[0].array().size == it[0].remaining()
        val array: ByteArray = if (simpleArray) {
            it[0].array()
        }
        else {
            Class.forName("org.xnio.Buffers", true, target::class.java.classLoader)
                .getMethod("take", Array<ByteBuffer>::class.java, Int::class.java, Int::class.java)
                .invoke(null, it, 0, it.size) as ByteArray
        }
        val buffer = array
            .also { logger.trace { "delegatedPooledResource: Payload received: ${it.decodeToString()}" } }
            .let(::retrievePayload)
            .also { storeHeaders(it.second) }
            .let(Pair<ByteArray, Map<String, String>>::first)
            .let(ByteBuffer::wrap)
        arrayOf(buffer)
    }

    private fun transformSession(ctClass: CtClass) {
        CtField.make(
            "private java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders = null;",
            ctClass
        ).also(ctClass::addField)
        CtMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.lang.String>*/ getHandshakeHeaders() {
                return this.handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        ctClass.constructors[0].insertCatching(
            CtBehavior::insertAfter,
            """
            this.handshakeHeaders = ${this::class.java.name}.INSTANCE.${this::getHandshakeHeaders.name}();
            ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(null);
            """.trimIndent()
        )
    }

    private fun transformSessionHandler(ctClass: CtClass) {
        val method = ctClass.getMethod("onConnect", "(Lio/undertow/websockets/spi/WebSocketHttpExchange;Lio/undertow/websockets/core/WebSocketChannel;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = $1.getRequestHeaders().keySet().iterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = headerNames.next();
                java.util.List/*<java.lang.String>*/ headerValues = $1.getRequestHeaders().get(headerName);
                java.lang.String header = java.lang.String.join(",", headerValues);
                allHeaders.put(headerName, header);
            }
            ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(allHeaders);
            """.trimIndent()
        )
    }

    private fun transformFrameHandler(ctClass: CtClass) {
        val onTextMethod = ctClass.getMethod("onText", "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/StreamSourceFrameChannel;)V")
        val onBinaryMethod = ctClass.getMethod("onBinary", "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/StreamSourceFrameChannel;)V")
        val invokeTextHandlerMethod = ctClass.getMethod("invokeTextHandler", "(Lio/undertow/websockets/core/BufferedTextMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V")
        val invokeBinaryHandlerMethod = ctClass.getMethod("invokeBinaryHandler", "(Lio/undertow/websockets/core/BufferedBinaryMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V")
        val storeHeadersCode =
            """
            if (this.session.getHandshakeHeaders() != null) {
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(this.session.getHandshakeHeaders());
            }
            """.trimIndent()
        val removeHeadersCode =
            """
            if (this.session.getHandshakeHeaders() != null) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        onTextMethod.insertCatching(CtBehavior::insertBefore, storeHeadersCode)
        onTextMethod.insertCatching(CtBehavior::insertAfter, removeHeadersCode)
        onBinaryMethod.insertCatching(CtBehavior::insertBefore, storeHeadersCode)
        onBinaryMethod.insertCatching(CtBehavior::insertAfter, removeHeadersCode)
        val textMessageProxy = createTextMessageProxy(ctClass.classPool).name
        val binaryMessageProxy = createBinaryMessageProxy(ctClass.classPool).name
        pooledProxyClass = createPooledProxy(ctClass.classPool)
        invokeTextHandlerMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                $1 = new ${textMessageProxy}($1);
            }
            """.trimIndent()
        )
        invokeBinaryHandlerMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                $1 = new ${binaryMessageProxy}($1);
            }
            """.trimIndent()
        )
    }

    private fun createTextMessageProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "io.undertow.websockets.core.BufferedTextMessage",
        "getData",
        ::delegatedTextMessageData,
        classPool
    ) { MethodCall.invoke(it.getConstructor(Boolean::class.java)).with(false) }

    private fun createBinaryMessageProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "io.undertow.websockets.core.BufferedBinaryMessage",
        "getData",
        ::delegatedBinaryMessageData,
        classPool
    ) { MethodCall.invoke(it.getConstructor(Boolean::class.java)).with(false) }

    private fun createPooledProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "org.xnio.Pooled",
        "getResource",
        ::delegatedPooledResource,
        classPool,
    ) { MethodCall.invoke(Any::class.java.getConstructor()) }

    private fun createDelegatedGetterProxy(
        className: String,
        delegatedMethod: String,
        delegateMethod: KCallable<*>,
        classPool: ClassPool,
        proxyName: String = "${className}Proxy",
        targetField: String = "target",
        constructorCall: (Class<*>) -> MethodCall
    ) = Class.forName(className, true, classPool.classLoader).let { clazz ->
        proxyClassCache.findOrInsert(clazz.classLoader, proxyName) {
            byteBuddy.subclass(clazz)
                .name(proxyName)
                .modifiers(Visibility.PUBLIC)
                .defineField(targetField, clazz, Visibility.PRIVATE)
                .defineConstructor(Visibility.PUBLIC)
                .withParameter(clazz)
                .intercept(constructorCall(clazz).andThen(FieldAccessor.ofField(targetField).setsArgumentAt(0)))
                .method(ElementMatchers.isPublic<MethodDescription>().and(ElementMatchers.isDeclaredBy(clazz)))
                .intercept(MethodCall.invokeSelf().onField(targetField).withAllArguments())
                .method(ElementMatchers.named<MethodDescription>(delegatedMethod).and(ElementMatchers.takesNoArguments()))
                .intercept(MethodDelegation.withDefaultConfiguration()
                    .filter(ElementMatchers.named(delegateMethod.name))
                    .to(this@UndertowWsTransformerObject))
                .make()
                .load(clazz.classLoader, ClassLoadingStrategy.Default.INJECTION)
                .also { classPool.appendClassPath(ByteArrayClassPath(proxyName, it.bytes)) }
                .loaded
        }
    }

}
