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
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import io.mockk.ConstantAnswer
import io.mockk.ManyAnswersAnswer
import io.mockk.MockKAnnotations
import io.mockk.ThrowingAnswer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class RetryingAgentMetadataSenderTest {

    @MockK
    private lateinit var messageTransport: AgentMessageTransport<String>
    @MockK
    private lateinit var responseStatus: ResponseStatus
    @MockK
    private lateinit var messageSerializer: AgentMessageSerializer<AgentMetadata, String>
    @MockK
    private lateinit var destinationMapper: AgentMessageDestinationMapper
    @MockK
    private lateinit var messageDestination: AgentMessageDestination
    @MockK
    private lateinit var stateListener: TransportStateListener

    private lateinit var sender: RetryingAgentMetadataSender<String>

    private val destination = slot<AgentMessageDestination>()

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        every { messageSerializer.serialize(any()) } returns "serialized"
        every { messageSerializer.contentType() } returns "test/test"
        every { destinationMapper.map(capture(destination)) } returns messageDestination
        every { stateListener.onStateAlive() } returns Unit
        sender = RetryingAgentMetadataSender(messageTransport, messageSerializer, destinationMapper)
        sender.addStateListener(stateListener)
    }

    @Test
    fun `sending successful for AgentMetadata`() {
        every { messageTransport.send(any(), any(), any()) } returns responseStatus
        every { responseStatus.success } returns true

        assertFalse(sender.metadataSent)
        val config = mockk<AgentMetadata>()
        val thread = sender.send(config)

        thread.join(5000)
        verify(exactly = 1) { messageSerializer.serialize(any()) }
        verify(exactly = 1) { messageSerializer.serialize(config) }
        verifyMethodCallsAfterSend(1, "serialized", "test/test")
    }

    @Test
    fun `sending successful for string`() {
        every { messageTransport.send(any(), any(), any()) } returns responseStatus
        every { responseStatus.success } returns true

        assertFalse(sender.metadataSent)
        val thread = sender.send("somestring")

        thread.join(5000)
        verifyMethodCallsAfterSend(1, "somestring", "test/test")
    }

    @Test
    fun `sending successful for string with content-type`() {
        every { messageTransport.send(any(), any(), any()) } returns responseStatus
        every { responseStatus.success } returns true

        assertFalse(sender.metadataSent)
        val thread = sender.send("somestring", "test/custom")

        thread.join(5000)
        verifyMethodCallsAfterSend(1, "somestring", "test/custom")
    }

    @Test
    fun `sending failed with error code then success`() {
        every { messageTransport.send(any(), any(), any()) } returns responseStatus
        every { responseStatus.success } returnsMany listOf(false, false, false, false, true)

        assertFalse(sender.metadataSent)
        val thread = sender.send(mockk<AgentMetadata>())

        Thread.sleep(1000)
        verifyMethodCallsBeforeSend("serialized", "test/test")

        thread.join(5000)
        verifyMethodCallsAfterSend(5, "serialized", "test/test")
    }

    @Test
    fun `sending failed with exception then success`() {
        every { messageTransport.send(any(), any(), any()) } answers ManyAnswersAnswer(listOf(
            ThrowingAnswer(IOException()),
            ThrowingAnswer(IOException()),
            ThrowingAnswer(IOException()),
            ThrowingAnswer(IOException()),
            ConstantAnswer(responseStatus)
        ))
        every { responseStatus.success } returns true

        assertFalse(sender.metadataSent)
        val thread = sender.send(mockk<AgentMetadata>())

        Thread.sleep(1000)
        verifyMethodCallsBeforeSend("serialized", "test/test")

        thread.join(5000)
        verifyMethodCallsAfterSend(5, "serialized", "test/test")
    }

    @Suppress("SameParameterValue")
    private fun verifyMethodCallsBeforeSend(payload: String, contentType: String) {
        verify(atLeast = 1) { messageTransport.send(messageDestination, payload, contentType) }
        verify(exactly = 0) { stateListener.onStateAlive() }
        assertFalse(sender.metadataSent)
    }

    private fun verifyMethodCallsAfterSend(sendCount: Int, payload: String, contentType: String) {
        verify(exactly = sendCount) { messageTransport.send(any(), any(), any()) }
        verify(exactly = sendCount) { messageTransport.send(messageDestination, payload, contentType) }
        verify(exactly = 1) { destinationMapper.map(any()) }
        verify(exactly = 1) { destinationMapper.map(destination.captured) }
        verify(exactly = 1) { stateListener.onStateAlive() }
        assertTrue(sender.metadataSent)
        assertEquals("PUT", destination.captured.type)
        assertEquals("agent-metadata", destination.captured.target)
    }

}
