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
package com.epam.drill.instrument.util

import javassist.*
import java.io.*
import java.security.*


inline fun createAndTransform(
    classBytes: ByteArray,
    loader: Any?,
    protectionDomain: Any?,
    transformer: (CtClass, ClassPool, ClassLoader?, ProtectionDomain?) -> ByteArray?,
): ByteArray? {
    val classPool = ClassPool(true)
    if (loader == null) {
        classPool.appendClassPath(LoaderClassPath(ClassLoader.getSystemClassLoader()))
    } else {
        classPool.appendClassPath(LoaderClassPath(loader as? ClassLoader))
    }

    val clazz = classPool.makeClass(ByteArrayInputStream(classBytes), false)
    clazz.defrost()

    return transformer(clazz, classPool, loader as? ClassLoader, protectionDomain as? ProtectionDomain)
}
