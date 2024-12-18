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
package com.epam.drill.agent.instrument

import kotlinx.cinterop.*
import org.objectweb.asm.ClassReader
import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.bits.loadByteArray
import io.ktor.utils.io.bits.of
import com.epam.drill.agent.instrument.transformers.clients.*
import com.epam.drill.agent.instrument.transformers.servers.*
import com.epam.drill.agent.instrument.transformers.jetty.*
import com.epam.drill.agent.instrument.transformers.netty.*
import com.epam.drill.agent.instrument.transformers.tomcat.*
import com.epam.drill.agent.instrument.transformers.undertow.*
import com.epam.drill.agent.jvmapi.gen.Allocate
import com.epam.drill.agent.jvmapi.gen.jint
import com.epam.drill.agent.jvmapi.gen.jintVar
import com.epam.drill.agent.jvmapi.gen.jobject

object ClassFileLoadHook {

    private val clientTransformers = listOf(
        JavaHttpClientTransformer,
        ApacheHttpClientTransformer,
        OkHttp3ClientTransformer,
        NettyHttpServerTransformer,
        NettyWsServerTransformer,
        NettyWsClientTransformer,
        NettyWsMessagesTransformer,
        SSLEngineTransformer,
        KafkaTransformer,
        JettyHttpServerTransformer,
        JettyWsServerTransformer,
        JettyWsClientTransformer,
        JettyWsMessagesTransformer,
        ReactorTransformer,
        SpringWebClientTransformer,
        UndertowHttpServerTransformer,
        UndertowWsServerTransformer,
        UndertowWsClientTransformer,
        UndertowWsMessagesTransformer,
        TomcatHttpServerTransformer,
        TomcatWsServerTransformer,
        TomcatWsClientTransformer,
        TomcatWsMessagesTransformer
    )

    @OptIn(ExperimentalForeignApi::class)
    operator fun invoke(
        loader: jobject?,
        clsName: CPointer<ByteVar>?,
        protectionDomain: jobject?,
        classDataLen: jint,
        classData: CPointer<UByteVar>?,
        newDataLen: CPointer<jintVar>?,
        newData: CPointer<CPointerVar<UByteVar>>?,
    ) {
        val className = clsName?.toKString()
        val isJavaHttpClass = className?.matches(Regex("(java|sun)/net/.*Http.*")) ?: false
        val isJavaSslClass = className?.matches(Regex("(java|sun)/.*/ssl/.*")) ?: false
        if (className == null || classData == null) return
        if (isBootstrapClassloading(loader, protectionDomain) && !isJavaHttpClass && !isJavaSslClass) return
        val classBytes = ByteArray(classDataLen).also {
            Memory.of(classData, classDataLen).loadByteArray(0, it)
        }
        val classReader = ClassReader(classBytes)
        var transformedBytes = classBytes
        clientTransformers.forEach {
            if (it.permit(classReader.className, classReader.superName, classReader.interfaces)) {
                transformedBytes = it.transform(className, transformedBytes, loader, protectionDomain)
            }
        }
        if (!transformedBytes.contentEquals(classBytes)) {
            convertToNativePointers(transformedBytes, newData, newDataLen)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isBootstrapClassloading(loader: jobject?, protectionDomain: jobject?) =
        loader == null || protectionDomain == null

    @OptIn(ExperimentalForeignApi::class)
    private fun convertToNativePointers(
        transformedBytes: ByteArray,
        newData: CPointer<CPointerVar<UByteVar>>?,
        newDataLen: CPointer<jintVar>?,
    ) {
        Allocate(transformedBytes.size.toLong(), newData)
        transformedBytes.forEachIndexed { index, byte ->
            newData!!.pointed.value!![index] = byte.toUByte()
        }
        newDataLen!!.pointed.value = transformedBytes.size
    }

}
