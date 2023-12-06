package com.epam.drill.common.agent.transport

interface ResponseStatus {
    fun isSuccess(): Boolean
    fun getStatusObject(): Any?
}
