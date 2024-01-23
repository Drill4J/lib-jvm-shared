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

import java.io.ByteArrayInputStream
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.LoaderClassPath
import mu.KLogger

abstract class AbstractTransformerObject : TransformerObject {

    protected abstract val logger: KLogger

    override fun permit(className: String?, superName: String?, interfaces: String?) =
        permit(className, superName, interfaces?.split(";")?.toTypedArray() ?: emptyArray())

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        throw NotImplementedError()

    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray? = ClassPool(true).run {
        val classLoader = loader ?: ClassLoader.getSystemClassLoader()
        this.appendClassPath(LoaderClassPath(classLoader as? ClassLoader))
        this.makeClass(ByteArrayInputStream(classFileBuffer), false).let {
            val logError: (Throwable) -> Unit = { e -> logger.error { "Error during instrumenting, class=${it.name}" } }
            it.defrost()
            it.runCatching(::transform).onFailure(logError)
            it.toBytecode()
        }
    }

    open fun transform(ctClass: CtClass): Unit =
        throw NotImplementedError()

    open fun logInjectingHeaders(headers: Map<String, String>) =
        logger.trace { "logInjectingHeaders: Adding headers: $headers" }

    open fun logError(exception: Throwable, message: String) =
        logger.error(exception) { "logError: $message" }

    protected open fun CtMethod.insertCatching(insert: CtMethod.(String) -> Unit, code: String) = try {
        insert(
            """
                try {
                    $code
                } catch (Throwable e) {
                    ${this@AbstractTransformerObject::class.java.name}.INSTANCE.${this@AbstractTransformerObject::logError.name}(e, "Error in the injected code, method name: $name.");
                }
            """.trimIndent()
        )
    } catch (e: Throwable) {
        logError(e, "insertCatching: Can't insert code, method name: $name")
    }

}
