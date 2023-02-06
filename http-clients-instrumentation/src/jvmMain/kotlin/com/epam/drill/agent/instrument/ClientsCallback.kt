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
package com.epam.drill.agent.instrument

object ClientsCallback {

    private const val DRILL_HEADER_PREFIX = "drill-"
    private const val SESSION_ID_HEADER = "${DRILL_HEADER_PREFIX}session-id"

    private var _requestCallback: (() -> Map<String, String>)? = null
    private var _responceCallback: ((Map<String, String>) -> Unit)? = null
    private var _sendConditionCallback: (() -> Boolean)? = null

    fun initRequestCallback(callback: () -> Map<String, String>) {
        _requestCallback = callback
    }

    fun initResponseCallback(callback: (Map<String, String>) -> Unit) {
        _responceCallback = callback
    }

    fun initSendConditionCallback(callback: () -> Boolean) {
        _sendConditionCallback = callback
    }

    fun getHeaders(): Map<String, String> = _requestCallback?.invoke() ?: emptyMap()

    fun storeHeaders(headers: Map<String, String>) = _responceCallback?.invoke(headers)

    fun isRequestCallbackSet() = _requestCallback != null

    fun isResponseCallbackSet() = _responceCallback != null

    fun isSendConditionCallbackSet() = _sendConditionCallback != null

    fun isSendCondition(): Boolean = _sendConditionCallback?.invoke() ?: getHeaders().run {
        isRequestCallbackSet() && isNotEmpty() && get(SESSION_ID_HEADER) != null
    }

}
