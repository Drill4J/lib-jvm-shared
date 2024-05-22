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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import io.mockk.ConstantAnswer
import io.mockk.FunctionAnswer
import io.mockk.ManyAnswersAnswer
import io.mockk.MockKAnnotations
import io.mockk.ThrowingAnswer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify

class QueuedAgentMessageMetadataSenderTest {

    private class TestAgentMessage(val msg: String) : AgentMessage()

    @MockK
    private lateinit var messageTransport: AgentMessageTransport<String>
    @MockK
    private lateinit var responseStatus: ResponseStatus
    @MockK
    private lateinit var messageSerializer: AgentMessageSerializer<AgentMessage, String>
    @MockK
    private lateinit var destinationMapper: AgentMessageDestinationMapper
    @MockK
    private lateinit var configSender: AgentMetadataSender<String>
    @MockK
    private lateinit var stateNotifier: TransportStateNotifier
    @MockK
    private lateinit var stateListener: TransportStateListener
    @MockK
    private lateinit var messageQueue: InMemoryAgentMessageQueue<String>

    private lateinit var sender: QueuedAgentMessageSender<AgentMessage, String>

    private val incomingMessage = mutableListOf<TestAgentMessage>()
    private val incomingDestinations = mutableListOf<AgentMessageDestination>()
    private val toSendMessages = mutableListOf<String>()
    private val toSendContentTypes = mutableListOf<String>()
    private val toSendDestinations = mutableListOf<AgentMessageDestination>()
    private val queuedPairs = mutableListOf<Pair<AgentMessageDestination, String>>()

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        val serialize: (TestAgentMessage) -> String = {
            "serialized-${it.msg}"
        }
        val mapDestination: (AgentMessageDestination) -> AgentMessageDestination = {
            AgentMessageDestination("MAP", "mapped-${it.target}")
        }
        every { messageSerializer.serialize(capture(incomingMessage)) } answers
                FunctionAnswer { serialize(it.invocation.args[0] as TestAgentMessage) }
        every { messageSerializer.contentType() } returns "test/test"
        every { destinationMapper.map(capture(incomingDestinations)) } answers
                FunctionAnswer { mapDestination(it.invocation.args[0] as AgentMessageDestination) }
        every { configSender.addStateListener(any()) } returns Unit
        every { stateNotifier.addStateListener(any()) } returns Unit
        every { stateListener.onStateFailed() } returns Unit
        every { messageQueue.offer(capture(queuedPairs)) } returns true

        sender = QueuedAgentMessageMetadataSender(messageTransport, messageSerializer, destinationMapper, configSender,
            stateNotifier, stateListener, messageQueue)

        incomingMessage.clear()
        incomingDestinations.clear()
        toSendMessages.clear()
        toSendContentTypes.clear()
        toSendDestinations.clear()
        queuedPairs.clear()
    }

    @Test
    fun `sending successful`() {
        every { responseStatus.success } returns true
        every {messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} returns responseStatus

        val responses = mutableListOf<ResponseStatus>()
        for (i in 0..9) sender.send(AgentMessageDestination("TYPE", "target-$i"), TestAgentMessage("message-$i"))
            .also(responses::add)

        for (i in 0..9) {
            assertTrue(responses[i].success)

            assertEquals("TYPE", incomingDestinations[i].type)
            assertEquals("target-$i", incomingDestinations[i].target)
            assertEquals("message-$i", incomingMessage[i].msg)

            assertEquals("MAP", toSendDestinations[i].type)
            assertEquals("mapped-target-$i", toSendDestinations[i].target)
            assertEquals("serialized-message-$i", toSendMessages[i])
            assertEquals("test/test", toSendContentTypes[i])
        }

        verifyInitialization()
        verifyMethodCalls(receive = 10, send = 10, queue = 0, dequeue = 0, failed = 0)
    }

    @Test
    fun `sending successful for error response`() {
        every { responseStatus.success } returns false
        every { messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} returns responseStatus

        val responses = mutableListOf<ResponseStatus>()
        for (i in 0..9) sender.send(AgentMessageDestination("TYPE", "target-$i"), TestAgentMessage("message-$i"))
            .also(responses::add)

        for (i in 0..9) {
            assertFalse(responses[i].success)

            assertEquals("TYPE", incomingDestinations[i].type)
            assertEquals("target-$i", incomingDestinations[i].target)
            assertEquals("message-$i", incomingMessage[i].msg)

            assertEquals("MAP", toSendDestinations[i].type)
            assertEquals("mapped-target-$i", toSendDestinations[i].target)
            assertEquals("serialized-message-$i", toSendMessages[i])
            assertEquals("test/test", toSendContentTypes[i])
        }

        verifyInitialization()
        verifyMethodCalls(receive = 10, send = 10, queue = 0, dequeue = 0, failed = 0)
    }

    @Test
    fun `sending queued for exception response`() {
        every {messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} answers ThrowingAnswer(IOException())

        val responses = mutableListOf<ResponseStatus>()
        for (i in 0..9) sender.send(AgentMessageDestination("TYPE", "target-$i"), TestAgentMessage("message-$i"))
            .also(responses::add)

        assertEquals("MAP", toSendDestinations[0].type)
        assertEquals("mapped-target-0", toSendDestinations[0].target)
        assertEquals("serialized-message-0", toSendMessages[0])
        assertEquals("test/test", toSendContentTypes[0])

        for (i in 0..9) {
            assertFalse(responses[i].success)

            assertEquals("TYPE", incomingDestinations[i].type)
            assertEquals("target-$i", incomingDestinations[i].target)
            assertEquals("message-$i", incomingMessage[i].msg)

            assertEquals("MAP", queuedPairs[i].first.type)
            assertEquals("mapped-target-$i", queuedPairs[i].first.target)
            assertEquals("serialized-message-$i", queuedPairs[i].second)
        }

        verifyInitialization()
        verifyMethodCalls(receive = 10, send = 1, queue = 10, dequeue = 0, failed = 1)
    }

    @Test
    fun `queue sent successful after state-alive`() {
        every { responseStatus.success } returns true
        every { messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} returns responseStatus
        fillMessageQueue(10)

        sender.onStateAlive()
        sender.sendingThread?.join(5000)

        for (i in 0..9) {
            assertEquals("MAP", toSendDestinations[i].type)
            assertEquals("queued-target-$i", toSendDestinations[i].target)
            assertEquals("queued-message-$i", toSendMessages[i])
            assertEquals("test/test", toSendContentTypes[i])
        }

        verifyInitialization()
        verifyMethodCalls(receive = 0, send = 10, queue = 0, dequeue = 10, failed = 0)
    }

    @Test
    fun `queue send failed after state-alive`() {
        every { responseStatus.success } returns true
        every { messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} answers ThrowingAnswer(IOException())
        fillMessageQueue(10)

        sender.onStateAlive()
        sender.sendingThread?.join(5000)

        assertEquals("MAP", toSendDestinations[0].type)
        assertEquals("queued-target-0", toSendDestinations[0].target)
        assertEquals("queued-message-0", toSendMessages[0])
        assertEquals("test/test", toSendContentTypes[0])

        verifyInitialization()
        verifyMethodCalls(receive = 0, send = 1, queue = 0, dequeue = 0, failed = 1)
    }

    @Test
    fun `queue send partially after state-alive`() {
        every { responseStatus.success } returns true
        every { messageTransport.send(
            capture(toSendDestinations),
            capture(toSendMessages),
            capture(toSendContentTypes)
        )} answers ManyAnswersAnswer(listOf(
            ConstantAnswer(responseStatus),
            ConstantAnswer(responseStatus),
            ConstantAnswer(responseStatus),
            ThrowingAnswer(IOException())
        ))
        fillMessageQueue(10)

        sender.onStateAlive()
        sender.sendingThread?.join(5000)

        for (i in 0..3) {
            assertEquals("MAP", toSendDestinations[i].type)
            assertEquals("queued-target-$i", toSendDestinations[i].target)
            assertEquals("queued-message-$i", toSendMessages[i])
            assertEquals("test/test", toSendContentTypes[i])
        }

        verifyInitialization()
        verifyMethodCalls(receive = 0, send = 4, queue = 0, dequeue = 3, failed = 1)
    }

    private fun verifyInitialization() {
        verify(exactly = 1) { configSender.addStateListener(any()) }
        verify(exactly = 1) { configSender.addStateListener(sender) }
        verify(exactly = 1) { stateNotifier.addStateListener(any()) }
        verify(exactly = 1) { stateNotifier.addStateListener(sender) }
    }

    private fun verifyMethodCalls(receive: Int, send: Int, queue: Int, dequeue: Int, failed: Int) {
        verify(exactly = receive) { messageSerializer.serialize(any()) }
        verify(exactly = receive) { destinationMapper.map(any()) }
        verify(exactly = send) { messageTransport.send(any(), any(), any()) }
        verify(exactly = queue) { messageQueue.offer(any()) }
        verify(exactly = dequeue) { messageQueue.remove() }
        verify(exactly = failed) { stateListener.onStateFailed() }
    }

    @Suppress("SameParameterValue")
    private fun fillMessageQueue(size: Int) {
        val queuePair: (Int) -> Pair<AgentMessageDestination, String> = {
            Pair(AgentMessageDestination("MAP", "queued-target-$it"), "queued-message-$it")
        }
        val queue = ConcurrentLinkedQueue<Pair<AgentMessageDestination, String>>()
        for (i in 1..size) queue.add(queuePair(i - 1))
        every { messageQueue.peek() } answers FunctionAnswer { queue.peek() }
        every { messageQueue.element() } answers FunctionAnswer { queue.element() }
        every { messageQueue.poll() } answers FunctionAnswer { queue.poll() }
        every { messageQueue.remove() } answers FunctionAnswer { queue.remove() }
    }

}
