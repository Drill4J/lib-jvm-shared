package de.nielsfalk.ktor.swagger

import com.google.gson.GsonBuilder
import io.ktor.content.TextContent
import io.ktor.http.ContentType

private val gson = GsonBuilder().create()

internal fun Any.toJsonContent(): TextContent = TextContent(
    text = let(gson::toJson),
    contentType = ContentType.Application.Json
)
