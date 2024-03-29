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
package com.epam.drill.agent.configuration

import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import com.benasher44.uuid.uuid4
import com.epam.drill.common.agent.configuration.AgentConfig

actual object WsConfiguration {

    actual fun generateAgentConfigInstanceId() = run {
        if(agentConfig.instanceId.isEmpty()) agentConfig = agentConfig.copy(instanceId = uuid4().toString())
    }

    actual fun setRequestPattern(pattern: String?) = run { requestPattern = pattern }

    actual fun getAgentConfigHexString() = agentConfig.copy(parameters = emptyMap()).run {
        ProtoBuf.encodeToHexString(AgentConfig.serializer(), this)
    }

    actual fun getSslTruststore() = agentParameters.sslTruststore

    actual fun getSslTruststorePassword() = agentParameters.sslTruststorePassword

    actual fun getDrillInstallationDir() = agentParameters.drillInstallationDir

}
