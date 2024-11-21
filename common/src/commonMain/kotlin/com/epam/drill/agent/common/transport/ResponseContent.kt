package com.epam.drill.agent.common.transport

interface ResponseContent<T>: ResponseStatus {
    val content: T
}