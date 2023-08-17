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
package com.epam.drill.agent.ws

import kotlinx.serialization.serializer
import kotlinx.atomicfu.update
import com.epam.drill.agent.ws.topic.Topic
import com.epam.drill.agent.ws.topic.GenericTopic
import com.epam.drill.agent.ws.topic.InfoTopic
import com.epam.drill.agent.ws.topic.mapper

@ThreadLocal
object WsRouter {

    operator fun invoke(alotoftopics: WsRouter.() -> Unit) {
        alotoftopics(this)
    }

    operator fun get(topic: String): Topic? {
        return mapper.value[topic]
    }

    inline fun <reified Generic : Any> rawTopic(destination: String, noinline block: suspend (Generic) -> Unit) {
        val infoTopic = GenericTopic(destination, Generic::class.serializer(), block)
        val mapping: Pair<String, Topic> = destination to infoTopic
        mapper.update { it + mapping }
    }

    fun rawTopic(destination: String, block: suspend (String) -> Unit) {
        val infoTopic = InfoTopic(destination, block)
        val mapping: Pair<String, Topic> = destination to infoTopic
        mapper.update { it + mapping }
    }

}
