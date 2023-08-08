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
package com.epam.drill.agent.ws.send

import kotlinx.serialization.json.Json
import com.epam.drill.common.message.DrillMessage
import com.epam.drill.common.message.DrillMessageWrapper
import com.epam.drill.common.message.Message
import com.epam.drill.common.message.MessageType

fun sendMessage(pluginId: String, content: String) {
    Sender.send(
        Message(
            type = MessageType.PLUGIN_DATA,
            destination = "",
            data = (Json.encodeToString(
                DrillMessageWrapper.serializer(),
                DrillMessageWrapper(pluginId, DrillMessage(content = content))
            )).encodeToByteArray()
        )
    )
}
