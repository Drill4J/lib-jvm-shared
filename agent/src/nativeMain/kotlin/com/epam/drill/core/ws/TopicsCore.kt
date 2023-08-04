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
package com.epam.drill.core.ws

import kotlin.native.concurrent.freeze
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

@SharedImmutable
private val topicContext = newSingleThreadContext("topic's processor")

@SharedImmutable
val mapper = atomic(mapOf<String, Topic>().freeze()).freeze()

@ThreadLocal
object WsRouter {

    operator fun invoke(alotoftopics: WsRouter.() -> Unit) {
        alotoftopics(this)
    }

    operator fun get(topic: String): Topic? {
        return mapper.value[topic]
    }

}

@Suppress("UnusedReceiverParameter")
inline fun <reified Generic : Any> WsRouter.rawTopic(destination: String, noinline block: suspend (Generic) -> Unit) {
    val infoTopic = GenericTopic(destination, Generic::class.serializer(), block)
    val mapping: Pair<String, Topic> = destination to infoTopic
    mapper.update { it + mapping }
}

@Suppress("UnusedReceiverParameter")
fun WsRouter.rawTopic(destination: String, block: suspend (String) -> Unit) {
    val infoTopic = InfoTopic(destination, block)
    val mapping: Pair<String, Topic> = destination to infoTopic
    mapper.update { it + mapping }
}

open class Topic(open val destination: String)

class GenericTopic<T>(
    override val destination: String,
    private val deserializer: KSerializer<T>,
    val block: suspend (T) -> Unit
) : Topic(destination) {
    suspend fun deserializeAndRun(message: ByteArray) = withContext(topicContext) {
        block(ProtoBuf.decodeFromByteArray(deserializer, message))
    }
}

class InfoTopic(
    override val destination: String,
    private val block: suspend (String) -> Unit
) : Topic(destination) {
    suspend fun run(message: ByteArray) = withContext(topicContext) {
        block(message.decodeToString())
    }
}
