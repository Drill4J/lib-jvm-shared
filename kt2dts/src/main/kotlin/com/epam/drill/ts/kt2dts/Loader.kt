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
package com.epam.drill.ts.kt2dts

import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import java.io.*
import java.net.*
import java.util.jar.*
import kotlin.reflect.*
import kotlin.reflect.full.*

data class Descriptor(
    val klass: AnyKlass,
    val descriptor: SerialDescriptor,
    val descendants: Set<AnyKlass> = emptySet()
)

typealias AnyKlass = KClass<out Any>

@InternalSerializationApi
fun AnyKlass.descriptor(
    descendants: Set<AnyKlass>? = null
) = Descriptor(
    klass = this,
    descriptor = serializer().descriptor,
    descendants = descendants ?: emptySet()
)

fun findSerialDescriptors(
    extClassLoader: URLClassLoader? = null,
    filter: (String) -> Boolean = { true }
): Sequence<Descriptor> = run {
    val paths = System.getProperty("java.class.path").run {
        split(File.pathSeparatorChar).distinct().map(::File)
    }
    val extPaths = (extClassLoader?.urLs ?: emptyArray()).map { File(it.toURI()) }
    val classLoader = extClassLoader ?: Thread.currentThread().contextClassLoader
    val classes = (paths + extPaths).asSequence().findClassNames(filter).map {
        classLoader.loadClass(it).kotlin
    }.filter { it.findAnnotation<Serializable>()?.with == KSerializer::class }.toSet()
    val superClasses: Map<AnyKlass, Set<AnyKlass>> = classes.fold(mapOf()) { map, klass ->
        klass.superclasses.firstOrNull { it in classes }?.let {
            val set = (map[it] ?: emptySet()) + klass
            map + (it to set)
        } ?: map
    }
    superClasses to classes
}.let { (superClasses, classes) ->
    classes.map { it.descriptor(superClasses[it]) }
}.asSequence()

private fun Sequence<File>.findClassNames(
    filter: (String) -> Boolean = { true }
): Sequence<String> = flatMap { pathElement ->
    val suffix = "\$Companion.class"
    when {
        pathElement.isDirectory -> {
            val baseUri = pathElement.toURI()
            pathElement.walkTopDown().map(File::toURI)
                .filter { it.path.endsWith(suffix) }
                .map { baseUri.relativize(it).path }
                .map { it.toClassName(suffix) }
        }
        pathElement.name.endsWith(".jar") -> {
            JarFile(pathElement).entries().asSequence()
                .filter { it.name.endsWith(suffix) }
                .map { it.name.toClassName(suffix) }
        }
        else -> emptySequence()
    }
}.filter(filter).distinct()

private fun String.toClassName(suffix: String): String = replace(suffix, "").replace('/', '.')
