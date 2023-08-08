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
package com.epam.drill.instrument.http

import com.epam.drill.instrument.*
import com.epam.drill.instrument.util.*
import javassist.*
import java.security.*

actual object ApacheClient : TransformStrategy(), IStrategy {

    // TODO Waiting for this feature to move this permit to common part https://youtrack.jetbrains.com/issue/KT-20427
    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return interfaces.any { "org/apache/http/HttpClientConnection" == it }
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
            ctClass.getDeclaredMethod("sendRequestHeader").insertBefore(
                """
                    if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isSendCondition.name}()) { 
                        try {
                            java.util.Map headers = ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::getHeaders.name}();
                            java.util.Iterator iterator = headers.entrySet().iterator();             
                            while (iterator.hasNext()) {
                                java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                                $1.setHeader((String) entry.getKey(), (String) entry.getValue());
                            }
                            ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}(headers);
                        } catch (Exception e) {};
                    }
                """.trimIndent()
            )
            ctClass.getDeclaredMethod("receiveResponseEntity").insertBefore(
                """
                    if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isResponseCallbackSet.name}()) {
                        java.util.Map allHeaders = new java.util.HashMap();
                        java.util.Iterator iterator = $1.headerIterator();
                        while (iterator.hasNext()) {
                            org.apache.http.Header header = (org.apache.http.Header) iterator.next();
                            allHeaders.put(header.getName(), header.getValue());
                        }
                        ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::storeHeaders.name}(allHeaders);
                    }
            """.trimIndent())
        }
        return ctClass.toBytecode()
    }

}
