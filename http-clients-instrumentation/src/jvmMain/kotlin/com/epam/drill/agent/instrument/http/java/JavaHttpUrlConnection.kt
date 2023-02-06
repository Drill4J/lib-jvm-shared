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
package com.epam.drill.agent.instrument.http.java

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.util.*
import com.epam.drill.kni.*
import javassist.*
import org.objectweb.asm.*
import java.security.*

@Kni
actual object JavaHttpUrlConnection : TransformStrategy(), IStrategy {

    // TODO Waiting for this feature to move this permit to common part https://youtrack.jetbrains.com/issue/KT-20427
    actual override fun permit(classReader: ClassReader): Boolean {
        val parentClassName = runCatching { classReader.superName }.getOrDefault("")
        return parentClassName == "java/net/HttpURLConnection" ||
                parentClassName == "javax/net/ssl/HttpsURLConnection"
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? {
        return super.transform(className, classFileBuffer, loader, protectionDomain)
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        runCatching {
            ctClass.constructors.forEach {
                it.insertAfter(
                    """
                        if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isSendCondition.name}()) {
                            try {
                                java.util.Map headers = ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::getHeaders.name}();
                                java.util.Iterator iterator = headers.entrySet().iterator();                      
                                while (iterator.hasNext()) {
                                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                                    this.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
                                }
                                ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}(headers);   
                            } catch (Exception e) {};
                        }
                    """.trimIndent()
                )
            }
            ctClass.getMethod("getContent", "()Ljava/lang/Object;").insertAfter(
                """
                    if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isResponseCallbackSet.name}()) {
                        java.util.Map allHeaders = new java.util.HashMap();
                        java.util.Iterator iterator = this.getHeaderFields().keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = (String) iterator.next();
                            String value = this.getHeaderField(key);
                            allHeaders.put(key, value);
                        }
                        ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::storeHeaders.name}(allHeaders);
                }
            """.trimIndent())
        }.onFailure {
            logger.error(it) { "Error while instrumenting the class ${ctClass.name}" }
        }
        return ctClass.toBytecode()
    }
}
