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
package com.epam.drill.agent.instrument.servers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.Duration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.runner.RunWith
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.common.agent.request.DrillRequest

@RunWith(SpringRunner::class)
@EmbeddedKafka(
    partitions = 1,
    topics = ["test.producer.wosession", "test.producer.wsession", "test.listener.wosession", "test.listener.wsession"]
)
@ContextConfiguration(classes = [KafkaTransformerObjectTest.KafkaMessageListenerBean::class])
@Suppress("FunctionName")
class KafkaTransformerObjectTest {

    @Autowired
    private lateinit var kafkaBroker: EmbeddedKafkaBroker

    @Autowired
    private lateinit var beanFactory: BeanFactory

    @Test
    fun `test producer with empty headers`() = withKafka { producer, consumer ->
        TestRequestHolder.remove()
        producer.send(ProducerRecord("test.producer.wosession", "test", "test"))
        consumer.subscribe(setOf("test.producer.wosession"))
        val records = consumer.poll(Duration.ofSeconds(2))
        val record = records.records("test.producer.wosession").first()
        assertEquals(1, records.count())
        assertEquals("test", record.key())
        assertEquals("test", record.value())
        assertFalse(record.headers().headers("drill-session-id").iterator().hasNext())
        assertFalse(record.headers().headers("drill-header-data").iterator().hasNext())
    }

    @Test
    fun `test producer with session headers`() = withKafka { producer, consumer ->
        TestRequestHolder.store(DrillRequest("session-123", mapOf("drill-header-data" to "test-data")))
        producer.send(ProducerRecord("test.producer.wsession", "test", "test"))
        consumer.subscribe(setOf("test.producer.wsession"))
        val records = consumer.poll(Duration.ofSeconds(2))
        val record = records.records("test.producer.wsession").first()
        assertEquals(1, records.count())
        assertEquals("test", record.key())
        assertEquals("test", record.value())
        assertEquals("session-123", record.headers().headers("drill-session-id").first().value().decodeToString())
        assertEquals("test-data", record.headers().headers("drill-header-data").first().value().decodeToString())
    }

    @Test
    fun `test listener with empty headers`() = withKafka { producer, _ ->
        TestRequestHolder.remove()
        producer.send(ProducerRecord("test.listener.wosession", "test", "test"))
        Thread.sleep(2000)
        val records = beanFactory.getBean(KafkaMessageListenerBean::class.java).withoutSessionlistenerRecords
        val drillRequests = beanFactory.getBean(KafkaMessageListenerBean::class.java).withoutSessionlistenerDrillRequests
        assertEquals(1, records.count())
        assertEquals("test", records[0].key())
        assertEquals("test", records[0].value())
        assertFalse(records[0].headers().headers("drill-session-id").iterator().hasNext())
        assertFalse(records[0].headers().headers("drill-header-data").iterator().hasNext())
        assertTrue(drillRequests.isEmpty())
    }

    @Test
    fun `test listener with session headers`() = withKafka { producer, _ ->
        TestRequestHolder.remove()
        val headers = setOf(
            RecordHeader("drill-session-id", "session-456".encodeToByteArray()),
            RecordHeader("drill-header-data", "test-listener-data".encodeToByteArray())
        )
        producer.send(ProducerRecord("test.listener.wsession", 0, "test", "test", headers))
        Thread.sleep(2000)
        val records = beanFactory.getBean(KafkaMessageListenerBean::class.java).withSessionlistenerRecords
        val drillRequests = beanFactory.getBean(KafkaMessageListenerBean::class.java).withSessionlistenerDrillRequests
        assertEquals(1, records.count())
        assertEquals(1, drillRequests.count())
        assertEquals("test", records[0].key())
        assertEquals("test", records[0].value())
        assertEquals("session-456", records[0].headers().headers("drill-session-id").first().value().decodeToString())
        assertEquals("test-listener-data", records[0].headers().headers("drill-header-data").first().value().decodeToString())
        assertEquals("session-456", drillRequests[0].drillSessionId)
        assertEquals("test-listener-data", drillRequests[0].headers["drill-header-data"])
    }

    private fun withKafka(block: (KafkaProducer<String, String>, KafkaConsumer<String, String>) -> Unit) {
        val producer = createProducer()
        val consumer = createConsumer()
        block(producer, consumer)
        producer.close()
        consumer.close()
    }

    private fun createProducer(): KafkaProducer<String, String> {
        val producerProperties = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBroker.brokersAsString,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.qualifiedName
        )
        return KafkaProducer<String, String>(producerProperties)
    }

    private fun createConsumer(): KafkaConsumer<String, String> {
        val consumerProperties = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBroker.brokersAsString,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName,
            ConsumerConfig.GROUP_ID_CONFIG to "consumer-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )
        return KafkaConsumer<String, String>(consumerProperties)
    }

    @Configuration
    @EnableKafka
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    open class KafkaMessageListenerBean {

        val withoutSessionlistenerRecords = mutableListOf<ConsumerRecord<String, String>>()
        val withoutSessionlistenerDrillRequests = mutableListOf<DrillRequest>()
        val withSessionlistenerRecords = mutableListOf<ConsumerRecord<String, String>>()
        val withSessionlistenerDrillRequests = mutableListOf<DrillRequest>()

        @Autowired
        private lateinit var kafkaBroker: EmbeddedKafkaBroker

        @Bean
        open fun consumerFactory(): ConsumerFactory<String, String> {
            val consumerProperties = mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBroker.brokersAsString,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName,
                ConsumerConfig.GROUP_ID_CONFIG to "listener-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
            )
            return DefaultKafkaConsumerFactory(consumerProperties)
        }

        @Bean
        open fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
            val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
            factory.consumerFactory = consumerFactory()
            return factory
        }

        @KafkaListener(topics = ["test.listener.wosession"])
        fun listenWithoutSessionTopic(record: ConsumerRecord<String, String>) {
            withoutSessionlistenerRecords.add(record)
            TestRequestHolder.retrieve()?.also(withoutSessionlistenerDrillRequests::add)
        }

        @KafkaListener(topics = ["test.listener.wsession"])
        fun listenWithSessionTopic(record: ConsumerRecord<String, String>) {
            withSessionlistenerRecords.add(record)
            TestRequestHolder.retrieve()?.also(withSessionlistenerDrillRequests::add)
        }

    }

}
