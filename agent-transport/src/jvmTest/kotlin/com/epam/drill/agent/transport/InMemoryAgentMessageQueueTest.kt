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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import com.epam.drill.common.agent.transport.AgentMessageDestination

class InMemoryAgentMessageQueueTest {

    @MockK
    private lateinit var messageSerializer: AgentMessageSerializer<String>
    @MockK
    private lateinit var messageDestination: AgentMessageDestination

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        every { messageSerializer.sizeOf(any<AgentMessageDestination>()) } returns 10
        every { messageSerializer.sizeOf(any<String>()) } returns 100
    }

    @Test
    fun `add to non-full`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.add(Pair(messageDestination, "somestring"))

        assertEquals(10, queue.size())
        assertEquals(110L * 10, queue.bytesSize())
    }

    @Test
    fun `offer to non-full`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring"))

        assertEquals(10, queue.size())
        assertEquals(110L * 10, queue.bytesSize())
    }

    @Test
    fun `remove from non-empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring$i"))

        val removed1 = queue.remove()
        verifyQueueElement(queue, 9, removed1, "somestring1")

        val removed2 = queue.remove()
        verifyQueueElement(queue, 8, removed2, "somestring2")
    }

    @Test
    fun `poll from non-empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring$i"))

        val polled1 = queue.poll()
        verifyQueueElement(queue, 9, polled1, "somestring1")

        val polled2 = queue.poll()
        verifyQueueElement(queue, 8, polled2, "somestring2")
    }

    @Test
    fun `element from non-empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring$i"))

        val element1 = queue.element()
        verifyQueueElement(queue, 10, element1, "somestring1")

        val element2 = queue.element()
        verifyQueueElement(queue, 10, element2, "somestring1")
    }

    @Test
    fun `peek from non-empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring$i"))

        val element1 = queue.peek()
        verifyQueueElement(queue, 10, element1, "somestring1")

        val element2 = queue.peek()
        verifyQueueElement(queue, 10, element2, "somestring1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add to full`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1090)
        for(i in 1..9) queue.add(Pair(messageDestination, "somestring"))

        assertEquals(9, queue.size())
        assertEquals(110L * 9, queue.bytesSize())

        queue.add(Pair(messageDestination, "somestring"))
    }

    @Test
    fun `offer to full`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1090)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring"))

        assertEquals(9, queue.size())
        assertEquals(110L * 9, queue.bytesSize())
    }

    @Test(expected = NoSuchElementException::class)
    fun `remove from empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        queue.remove()
    }

    @Test
    fun `poll from empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        assertNull(queue.poll())
    }

    @Test(expected = NoSuchElementException::class)
    fun `element from empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        queue.element()
    }

    @Test
    fun `peek from empty`() {
        val queue = InMemoryAgentMessageQueue(messageSerializer, 1100)
        assertNull(queue.peek())
    }

    private fun verifyQueueElement(
        queue: InMemoryAgentMessageQueue<String>,
        size: Int,
        element: Pair<AgentMessageDestination, String>?,
        value: String
    ) {
        assertEquals(size, queue.size())
        assertEquals(size * 110L, queue.bytesSize())
        assertEquals(messageDestination, element?.first)
        assertEquals(value, element?.second)
    }

}
