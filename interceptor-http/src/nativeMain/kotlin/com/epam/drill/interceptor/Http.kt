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
package com.epam.drill.interceptor

import kotlin.native.concurrent.SharedImmutable

internal const val HTTP_DETECTOR_BYTES_COUNT = 8

internal const val HTTP_RESPONSE_MARKER = "HTTP"

internal const val FIRST_INDEX = 0

@SharedImmutable
internal val HTTP_VERBS =
    setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI") + HTTP_RESPONSE_MARKER
