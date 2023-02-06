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
package com.epam.drill

import java.util.jar.*


private fun getClassName(je: JarEntry): String {
    var className = je.name.substring(0, je.name.length - 6)
    className = className.replace('/', '.')
    return className
}

fun retrieveApiClass(targetClass: Class<*>, entrySet: Set<JarEntry>, cl: ClassLoader): Class<*>? {

    entrySet.filter { it.name.endsWith(".class") && !it.name.contains("$") }.map { je ->
        val className = getClassName(je)
        val basClass = cl.loadClass(className)
        var parentClass = basClass
        while (parentClass != null) {
            if (parentClass == targetClass) {
                return basClass
            }
            parentClass = parentClass.superclass
        }
        return@map
    }
    return null
}
