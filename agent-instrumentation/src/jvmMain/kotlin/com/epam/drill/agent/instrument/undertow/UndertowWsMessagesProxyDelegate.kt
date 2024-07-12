package com.epam.drill.agent.instrument.undertow

import kotlin.reflect.KCallable
import java.lang.reflect.Method
import java.nio.ByteBuffer
import javassist.ByteArrayClassPath
import javassist.ClassPool
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
import com.epam.drill.agent.instrument.PayloadProcessor

class UndertowWsMessagesProxyDelegate(
    private val payloadProcessor: PayloadProcessor
) {

    private val logger = KotlinLogging.logger {}
    private val byteBuddy by lazy(::ByteBuddy)
    private val proxyClassCache = TypeCache<String>()
    private var pooledProxyClass: Class<*>? = null
    private var textMessageProxyClass: Class<*>? = null
    private var binaryMessageProxyClass: Class<*>? = null

    @RuntimeType
    fun delegatedTextMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as String)
        .also { logger.trace { "delegatedTextMessageData: Payload received: $it" } }
        .let(payloadProcessor::retrieveDrillHeaders)

    @RuntimeType
    fun delegatedBinaryMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = method.invoke(target).let { pooledProxyClass!!.constructors[0].newInstance(it)!! }

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
            .let(payloadProcessor::retrieveDrillHeaders)
            .let(ByteBuffer::wrap)
            .let { arrayOf(it) }
    }

    fun getTextMessageProxy(classPool: ClassPool) = textMessageProxyClass
        ?: createTextMessageProxy(classPool).also(::textMessageProxyClass::set)

    fun getBinaryMessageProxy(classPool: ClassPool) = binaryMessageProxyClass
        ?: createBinaryMessageProxy(classPool).also(::binaryMessageProxyClass::set).also { getPooledProxy(classPool) }

    private fun getPooledProxy(classPool: ClassPool) = pooledProxyClass
        ?: createPooledProxy(classPool).also(::pooledProxyClass::set)

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
    ): Class<*> = Class.forName(className, true, classPool.classLoader).let { clazz ->
        proxyClassCache.findOrInsert(clazz.classLoader, proxyName) {
            byteBuddy.subclass(clazz)
                .name(proxyName)
                .modifiers(Visibility.PUBLIC)
                .defineField(targetField, clazz, Visibility.PRIVATE)
                .defineConstructor(Visibility.PUBLIC)
                .withParameter(clazz)
                .intercept(constructorCall(clazz)
                    .andThen(FieldAccessor.ofField(targetField).setsArgumentAt(0)))
                .method(
                    ElementMatchers.isPublic<MethodDescription>()
                    .and(ElementMatchers.isDeclaredBy(clazz)))
                .intercept(MethodCall.invokeSelf().onField(targetField).withAllArguments())
                .method(
                    ElementMatchers.named<MethodDescription>(delegatedMethod)
                    .and(ElementMatchers.takesNoArguments()))
                .intercept(
                    MethodDelegation.withDefaultConfiguration()
                    .filter(ElementMatchers.named(delegateMethod.name))
                    .to(this@UndertowWsMessagesProxyDelegate))
                .make()
                .load(clazz.classLoader, ClassLoadingStrategy.Default.INJECTION)
                .also { classPool.appendClassPath(ByteArrayClassPath(proxyName, it.bytes)) }
                .loaded
        }
    }

}