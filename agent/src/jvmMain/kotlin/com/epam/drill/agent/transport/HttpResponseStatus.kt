package com.epam.drill.agent.transport

import org.apache.hc.core5.http.HttpStatus
import com.epam.drill.common.agent.transport.ResponseStatus

class HttpResponseStatus(private val status: Int): ResponseStatus {
    override fun isSuccess() = status == HttpStatus.SC_SUCCESS
    override fun getStatusObject() = status
}
