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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.IOException
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import io.mockk.ConstantAnswer
import io.mockk.ManyAnswersAnswer
import io.mockk.MockKAnnotations
import io.mockk.ThrowingAnswer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify

class RetryingTransportStateNotifierTest {

    @MockK
    private lateinit var messageTransport: AgentMessageTransport<String>
    @MockK
    private lateinit var responseStatus: ResponseStatus
    @MockK
    private lateinit var messageSerializer: AgentMessageSerializer<AgentMessage, String>
    @MockK
    private lateinit var messageQueue: AgentMessageQueue<String>
    @MockK
    private lateinit var messagePair: Pair<AgentMessageDestination, String>
    @MockK
    private lateinit var messageDestination: AgentMessageDestination
    @MockK
    private lateinit var stateListener: TransportStateListener

    private lateinit var notifier: RetryingTransportStateNotifier<String>

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        every { messageTransport.send(any(), any(), any()) } answers
                ManyAnswersAnswer(listOf(
                    ThrowingAnswer(IOException()),
                    ThrowingAnswer(IOException()),
                    ThrowingAnswer(IOException()),
                    ThrowingAnswer(IOException()),
                    ConstantAnswer(responseStatus)
                ))
        every { messageSerializer.contentType() } returns "test/test"
        every { messageQueue.element() } returns messagePair
        every { messageQueue.remove() } returns messagePair
        every { messagePair.first } returns messageDestination
        every { messagePair.second } returns "somemessage"
        every { stateListener.onStateAlive() } returns Unit
        notifier = RetryingTransportStateNotifier(messageTransport, messageSerializer, messageQueue)
        notifier.addStateListener(stateListener)
    }

    @Test
    fun `retry successful for successful response`() {
        every { responseStatus.success } returns true

        assertFalse(notifier.retrying)
        assertNull(notifier.retryingThread)
        notifier.onStateFailed()

        Thread.sleep(1000)
        verifyMethodCallsBeforeSend()

        notifier.retryingThread?.join(5000)
        verifyMethodCallsAfterSend(5)
    }

    @Test
    fun `retry successful for error response`() {
        every { responseStatus.success } returns false

        assertFalse(notifier.retrying)
        assertNull(notifier.retryingThread)
        notifier.onStateFailed()

        Thread.sleep(1000)
        verifyMethodCallsBeforeSend()

        notifier.retryingThread?.join(5000)
        verifyMethodCallsAfterSend(5)
    }

    @Test
    fun `only one thread for multiple failures`() {
        every { responseStatus.success } returns true

        assertFalse(notifier.retrying)
        assertNull(notifier.retryingThread)

        val thread1 = notifier.onStateFailed().let { notifier.retryingThread }
        val thread2 = notifier.onStateFailed().let { notifier.retryingThread }

        Thread.sleep(1000)
        val thread3 = notifier.onStateFailed().let { notifier.retryingThread }
        verifyMethodCallsBeforeSend()

        notifier.retryingThread?.join(5000)
        assertEquals(thread1, thread2)
        assertEquals(thread1, thread3)
        verifyMethodCallsAfterSend(5)
    }

    @Test(expected = NoSuchElementException::class)
    fun `exception on empty queue`() {
        every { responseStatus.success } returns true
        every { messageQueue.element() } throws NoSuchElementException()
        every { messageQueue.remove() } throws NoSuchElementException()

        notifier.onStateFailed()
    }

    @Suppress("SameParameterValue")
    private fun verifyMethodCallsBeforeSend() {
        verify(atLeast = 1) { messageTransport.send(messageDestination, "somemessage", "test/test") }
        verify(exactly = 0) { stateListener.onStateAlive() }
        assertTrue(notifier.retrying)
        assertNotNull(notifier.retryingThread)
    }

    @Suppress("SameParameterValue")
    private fun verifyMethodCallsAfterSend(sendCount: Int) {
        verify(exactly = sendCount) { messageTransport.send(any(), any(), any()) }
        verify(exactly = sendCount) { messageTransport.send(messageDestination, "somemessage", "test/test") }
        verify(exactly = 1) { stateListener.onStateAlive() }
        assertFalse(notifier.retrying)
    }

}
