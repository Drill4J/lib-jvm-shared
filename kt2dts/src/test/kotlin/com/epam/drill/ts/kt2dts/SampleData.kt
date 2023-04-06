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
package com.epam.drill.ts.kt2dts

import kotlinx.serialization.*

@Serializable
data class Sample(
    val num: Int = 0,
    val str: String?
)

@Serializable
data class Complex(
    val num: Int,
    val list: List<String>,
    val optList: List<String?>,
    val listOfLists: List<List<Sample>>,
    val listOfOptLists: List<List<Sample>?>,
    val listOfListsOpt: List<List<Sample?>>,
    val mapOfLists: Map<String, List<String>>,
    val mapOfOptLists: Map<String, List<String>?>
)
