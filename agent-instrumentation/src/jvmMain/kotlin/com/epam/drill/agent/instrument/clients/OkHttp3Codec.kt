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

import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.ClientsCallback
import com.epam.drill.agent.instrument.TransformerObject

actual object OkHttp3Codec : TransformerObject, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        interfaces.any("okhttp3/internal/http/HttpCodec"::equals)

    override fun transform(className:String, ctClass: CtClass) {
        ctClass.getDeclaredMethod("writeRequestHeaders").insertBefore(
            """
            if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isSendCondition.name}()) {
                okhttp3.Request.Builder builder = $1.newBuilder();
                java.util.Map headers = ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::getHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    builder.addHeader((String) entry.getKey(), (String) entry.getValue());
                }
                $1 = builder.build();
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);                    
            }
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("openResponseBody").insertBefore(
            """
            if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isResponseCallbackSet.name}()) {
                java.util.Map allHeaders = new java.util.HashMap();
                java.util.Iterator iterator = $1.headers().names().iterator();
                while (iterator.hasNext()) { 
                    String key = (String) iterator.next();
                    String value = $1.headers().get(key);
                    allHeaders.put(key, value);
                }
                ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
    }

}
