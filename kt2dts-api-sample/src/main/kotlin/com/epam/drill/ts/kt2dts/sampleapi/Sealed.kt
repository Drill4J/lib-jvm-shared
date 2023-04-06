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
package com.epam.drill.ts.kt2dts.sampleapi

import kotlinx.serialization.*

@Serializable
sealed class Seal

@Serializable
@SerialName("s1")
data class Seal1(
    val payload: Int?
) : Seal()

@Serializable
@SerialName("s2")
data class Seal2(
    val payload: String?
) : Seal()

@Serializable
data class SealWrapper(val seal: Seal)
