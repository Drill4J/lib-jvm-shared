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
package com.epam.drill.agent.transport

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import com.epam.drill.common.agent.transport.AgentMessageDestination

class InMemoryAgentMessageQueue<T>(
    private val messageSerializer: AgentMessageSerializer<T>,
    private val capacity: Int
) : AgentMessageQueue<T> {

    private val queue: Queue<Pair<AgentMessageDestination, T>> = ConcurrentLinkedQueue()
    private var size: Int = 0

    override fun add(e: Pair<AgentMessageDestination, T>) = if (size < capacity) {
        queue.add(e).also { increaseSize(e) }
    } else {
        throw IllegalArgumentException("Queue full")
    }

    override fun offer(e: Pair<AgentMessageDestination, T>) = if (size < capacity) {
        queue.offer(e).also { if (it) increaseSize(e) }
    } else {
        false
    }

    override fun remove() = queue.remove().also(::decreaseSize)

    override fun poll() = queue.poll()?.also(::decreaseSize)

    override fun element() = queue.element()

    override fun peek() = queue.peek()

    private fun sizeOf(e: Pair<AgentMessageDestination, T>) =
        messageSerializer.sizeOf(e.first) + messageSerializer.sizeOf(e.second)

    private fun increaseSize(e: Pair<AgentMessageDestination, T>) {
        size += sizeOf(e)
    }

    private fun decreaseSize(e: Pair<AgentMessageDestination, T>) {
        size -= sizeOf(e)
    }

}
