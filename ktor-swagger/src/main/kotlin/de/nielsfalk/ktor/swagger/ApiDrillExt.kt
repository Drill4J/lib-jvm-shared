package de.nielsfalk.ktor.swagger

import io.ktor.application.ApplicationCall
import io.ktor.routing.Route
import io.ktor.util.pipeline.ContextDsl
import io.ktor.util.pipeline.PipelineContext

@ContextDsl
inline fun <reified LOCATION : Any> Route.post(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    return post<LOCATION, Unit>(metadata) { lc, _ -> body(lc) }
}

@ContextDsl
inline fun <reified LOCATION : Any> Route.put(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    return put<LOCATION, Unit>(metadata) { lc, _ -> body(lc) }
}

@ContextDsl
inline fun <reified LOCATION : Any> Route.patch(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route = patch<LOCATION, Unit>(metadata) { lc, _ -> body(lc) }
