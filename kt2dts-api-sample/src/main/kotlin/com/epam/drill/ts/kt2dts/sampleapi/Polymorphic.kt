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

@Polymorphic
@Serializable
abstract class Poly

@Serializable
@SerialName("ONE")
data class PolyOne(val one: Int) : Poly()

@Serializable
@SerialName("TWO")
data class PolyTwo(val two: String) : Poly()

@Serializable
@SerialName("SEAL")
data class PolySeal(val seal: Seal) : Poly()

@Serializable
data class PolyWrapper(val poly: Poly)
