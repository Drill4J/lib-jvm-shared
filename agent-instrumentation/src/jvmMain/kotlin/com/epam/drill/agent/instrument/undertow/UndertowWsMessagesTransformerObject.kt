package com.epam.drill.agent.instrument.undertow

import kotlin.reflect.KCallable
import java.lang.reflect.Method
import java.nio.ByteBuffer
import javassist.ByteArrayClassPath
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
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

abstract class UndertowWsMessagesTransformerObject : HeadersProcessor, PayloadProcessor, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}
    private val byteBuddy by lazy(::ByteBuddy)
    private val proxyClassCache = TypeCache<String>()
    private lateinit var pooledProxyClass: Class<*>

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) = listOf(
        "io/undertow/websockets/jsr/FrameHandler",
        "io/undertow/websockets/jsr/JsrWebSocketFilter",
        "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint\$BasicWebSocketSessionRemoteEndpoint",
        "io/undertow/websockets/core/WebSockets"
    ).contains(className) || "io/undertow/websockets/client/WebSocketClientHandshake" == superName

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting UndertowWsTransformer for $className..." }
        when (className) {
            "io/undertow/websockets/jsr/FrameHandler" -> transformFrameHandler(ctClass)
            "io/undertow/websockets/jsr/JsrWebSocketFilter" -> transformWebSocketFilter(ctClass)
            "io/undertow/websockets/jsr?WebSocketSessionRemoteEndpoint\$BasicWebSocketSessionRemoteEndpoint" -> transformBasicRemoteEndpoint(ctClass)
            "io/undertow/websockets/core/WebSockets" -> transformWebSockets(ctClass)
        }
        when (ctClass.superclass.name) {
            "io.undertow.websockets.client.WebSocketClientHandshake" -> transformClientHandshake(ctClass)
        }
    }

    private fun transformFrameHandler(ctClass: CtClass) {
        if (!isPayloadProcessingEnabled()) return
        val invokeTextHandlerMethod = ctClass.getMethod("invokeTextHandler", "(Lio/undertow/websockets/core/BufferedTextMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V")
        val invokeBinaryHandlerMethod = ctClass.getMethod("invokeBinaryHandler", "(Lio/undertow/websockets/core/BufferedBinaryMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V")
        val textMessageProxy = createTextMessageProxy(ctClass.classPool).name
        val binaryMessageProxy = createBinaryMessageProxy(ctClass.classPool).name
        pooledProxyClass = createPooledProxy(ctClass.classPool)
        val createProxyCode: (String) -> String = { proxy ->
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeHeaders())) {
                $1 = new $proxy($1);
            }
            """.trimIndent()
        }
        invokeTextHandlerMethod.insertCatching(CtBehavior::insertBefore, createProxyCode(textMessageProxy))
        invokeBinaryHandlerMethod.insertCatching(CtBehavior::insertBefore, createProxyCode(binaryMessageProxy))
    }

    private fun transformClientHandshake(ctClass: CtClass) {
        if(!isPayloadProcessingEnabled()) return
        ctClass.getMethod("createHeaders", "()Ljava/util/Map;").insertCatching(
            CtBehavior::insertAfter,
            """
            ${'$'}_.put("drill-ws-per-message", "true");
            """.trimIndent()
        )
    }

    private fun transformWebSocketFilter(ctClass: CtClass) {
        if (!isPayloadProcessingEnabled()) return
        val method = ctClass.getMethod("doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            ((javax.servlet.http.HttpServletResponse)$2).setHeader("drill-ws-per-message", "true");
            """.trimIndent()
        )
    }

    private fun transformBasicRemoteEndpoint(ctClass: CtClass) {
        if (!isPayloadProcessingEnabled()) return
        val propagateHandshakeHeaderCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}() && this.undertowSession.getHandshakeHeaders() != null) {
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                drillHeaders.put("drill-ws-per-message", this.undertowSession.getHandshakeHeaders().get("drill-ws-per-message"));
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
            }
            """.trimIndent()
        ctClass.getMethod("sendText", "(Ljava/lang.String;)V").insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
        ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;)V").insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
        ctClass.getMethod("sendObject", "(Ljava/lang/Object;)V").insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
        ctClass.getMethod("sendText", "(Ljava/lang.String;Z)V").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.undertowSession.getHandshakeHeaders())) {
                $1 = ${this::class.java.name}.INSTANCE.storeDrillHeaders($1);
            }
            """.trimIndent()
        )
        ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;Z)V").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.undertowSession.getHandshakeHeaders())) {
                byte[] modified = ${this::class.java.name}.INSTANCE.storeDrillHeaders(org.xnio.Buffers.take($1));
                $1.clear();
                $1 = java.nio.ByteBuffer.wrap(modified);
            }
            """.trimIndent()
        )
    }

    private fun transformWebSockets(ctClass: CtClass) {
        if (!isPayloadProcessingEnabled()) return
        val method = ctClass.getMethod("sendBlockingInternal", "(Ljava/nio/ByteBuffer;Lio/undertow/websockets/core/WebSocketFrameType;Lio/undertow/websockets/core/WebSocketChannel;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}())) {
                byte[] modified = ${this::class.java.name}.INSTANCE.storeDrillHeaders(org.xnio.Buffers.take($1));
                $1.clear();
                $1 = java.nio.ByteBuffer.wrap(modified);
            }
            """.trimIndent()
        )
    }

    @RuntimeType
    fun delegatedTextMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as String)
        .also { logger.trace { "delegatedTextMessageData: Payload received: $it" } }
        .let(::retrieveDrillHeaders)

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
    ) = (method.invoke(target) as Array<ByteBuffer>).let { buffers ->
        val isSimpleArray = buffers.size == 1 &&
                buffers[0].hasArray() &&
                buffers[0].arrayOffset() == 0 &&
                buffers[0].position() == 0 &&
                buffers[0].array().size == buffers[0].remaining()
        val array: ByteArray = if (isSimpleArray) {
            buffers[0].array()
        }
        else {
            Class.forName("org.xnio.Buffers", true, target::class.java.classLoader)
                .getMethod("take", Array<ByteBuffer>::class.java, Int::class.java, Int::class.java)
                .invoke(null, buffers, 0, buffers.size) as ByteArray
        }
        array.also { logger.trace { "delegatedPooledResource: Payload received: ${it.decodeToString()}" } }
            .let(::retrieveDrillHeaders)
            .let(ByteBuffer::wrap)
            .let { arrayOf(it) }
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
                .intercept(constructorCall(clazz)
                    .andThen(FieldAccessor.ofField(targetField).setsArgumentAt(0)))
                .method(ElementMatchers.isPublic<MethodDescription>()
                    .and(ElementMatchers.isDeclaredBy(clazz)))
                .intercept(MethodCall.invokeSelf().onField(targetField).withAllArguments())
                .method(ElementMatchers.named<MethodDescription>(delegatedMethod)
                    .and(ElementMatchers.takesNoArguments()))
                .intercept(MethodDelegation.withDefaultConfiguration()
                    .filter(ElementMatchers.named(delegateMethod.name))
                    .to(this@UndertowWsMessagesTransformerObject))
                .make()
                .load(clazz.classLoader, ClassLoadingStrategy.Default.INJECTION)
                .also { classPool.appendClassPath(ByteArrayClassPath(proxyName, it.bytes)) }
                .loaded
        }
    }

}
