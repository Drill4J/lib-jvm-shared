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
package com.epam.drill.agent.transport.http

import kotlin.test.Test
import kotlin.test.assertEquals
import com.epam.drill.common.agent.transport.AgentMessageDestination

class HttpAgentMessageDestinationMapperTest {

    private val mapper = HttpAgentMessageDestinationMapper("someAgentId", "someBuildVer")

    @Test
    fun `map AgentConfig`() {
        val destination = AgentMessageDestination("SOME", "agent-config")
        val mapped = mapper.map(destination)
        assertEquals("agents", mapped.target)
        assertEquals("SOME", mapped.type)
    }

    @Test
    fun `map AgentMessage`() {
        val destination = AgentMessageDestination("SOME", "something-else")
        val mapped = mapper.map(destination)
        assertEquals("agents/someAgentId/builds/someBuildVer/something-else", mapped.target)
        assertEquals("SOME", mapped.type)
    }

}
