package de.nielsfalk.ktor.swagger

import io.ktor.locations.Location
import kotlin.test.Test
import kotlin.test.assertEquals

class AnnotationDrillExtTest {
    @Location("/api")
    object Api {
        @Location("/foo/{bar}")
        data class Foo(val bar: String)
    }

    @Test
    fun `toLocation inner classes processed correctly`() {
        val location = Api.Foo::class.toLocation()
        assertEquals("/api/foo/{bar}", location.path)
    }
}
