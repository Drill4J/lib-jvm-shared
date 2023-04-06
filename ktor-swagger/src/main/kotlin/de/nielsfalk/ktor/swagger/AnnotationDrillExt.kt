@file:Suppress("EXPERIMENTAL_API_USAGE")

package de.nielsfalk.ktor.swagger

import io.ktor.locations.Location
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

internal fun KClass<*>.toLocation(): Location = javaObjectType.name.run {
    foldIndexed(StringBuilder()) { i, acc, c ->
        when {
            c == '$' -> 0 until i
            i == lastIndex -> 0..i
            else -> IntRange.EMPTY
        }.let(::slice).takeIf(String::isNotEmpty)
            ?.let { className ->
                val klass = ClassLoader.getSystemClassLoader().loadClass(className).kotlin
                val path = klass.findAnnotation<Location>()?.path
                path?.let(acc::append)
            }
        acc
    }.let { pathBuilder -> Location::class.primaryConstructor!!.call("$pathBuilder") }
}
