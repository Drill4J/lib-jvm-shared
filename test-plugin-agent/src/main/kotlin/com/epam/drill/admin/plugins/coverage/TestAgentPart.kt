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
package com.epam.drill.admin.plugins.coverage

import com.epam.drill.common.agent.*

@Suppress("unused")
class TestAgentPart constructor(
    id: String,
    agentContext: AgentContext,
    sender: Sender
) : AgentModule<String>(id, agentContext, sender) {

    override fun on() {
        send("xx")
    }

    override fun load() {
        println("Plugin $id initialized.")
    }

    override fun doAction(action: String): Any {
        println(action)
        return "action"
    }

    override fun parseAction(rawAction: String) = rawAction
}
