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

import com.epam.drill.agent.instrument.TestRequestHolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyRouteProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.comparator.Comparators
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import kotlin.test.Test

@RunWith(SpringRunner::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [ReactorTransformerObjectTest.SimpleController::class]
)
class ReactorTransformerObjectTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `given Mono class, MonoTransformerObject must propagate drill context`() {
        webTestClient.get().uri("/mono")
            .header("drill-session-id", "session-1")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("mono-session-1")
    }

    @Test
    fun `given Flux class, FluxTransformerObject must propagate drill context`() {
        webTestClient.get().uri("/flux")
            .header("drill-session-id", "session-1")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<String>>()
            .isEqualTo(listOf("flux-1-session-1", "flux-2-session-1", "flux-3-session-1"))
    }

    @Test
    fun `given ParallelFlux class, ParallelFluxTransformerObject must propagate drill context`() {
        webTestClient.get().uri("/parallel-flux")
            .header("drill-session-id", "session-1")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<String>>()
            .isEqualTo(listOf("parallel-1-session-1", "parallel-2-session-1", "parallel-3-session-1"))
    }

    @Test
    fun `given scheduled task, SchedulersTransformerObject must propagate drill context`() {
        webTestClient.get().uri("/scheduled-task")
            .header("drill-session-id", "session-1")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("task-session-1")
    }



    @RestController
    @EnableAutoConfiguration
    @Configuration
    open class SimpleController {

        @GetMapping("/mono")
        fun getMono(): Mono<String> {
            return Mono.just("mono")
                .subscribeOn(Schedulers.single())
                .map { "$it-${TestRequestHolder.retrieve()?.drillSessionId}" }
        }

        @GetMapping("/flux")
        fun getFlux(): Mono<List<String>> {
            return Flux.just("flux-1", "flux-2", "flux-3")
                .subscribeOn(Schedulers.single())
                .map { "$it-${TestRequestHolder.retrieve()?.drillSessionId}" }
                .collectList()
        }

        @GetMapping("/parallel-flux")
        fun getParallelFlux(): Mono<List<String>> {
            return Flux.just("parallel-1", "parallel-2", "parallel-3")
                .parallel()
                .runOn(Schedulers.single())
                .map { "$it-${TestRequestHolder.retrieve()?.drillSessionId}" }
                .collectSortedList(Comparators.comparable())
        }

        @GetMapping("/scheduled-task")
        fun getScheduledTask(): Mono<String> {
            return Mono.just("task")
                .delaySubscription(Duration.ofMillis(10), Schedulers.single())
                .map { "$it-${TestRequestHolder.retrieve()?.drillSessionId}" }
        }

        @Bean
        open fun nettyReactiveWebServerFactory(
            resourceFactory: ReactorResourceFactory?,
            routes: ObjectProvider<NettyRouteProvider?>
        ): NettyReactiveWebServerFactory {
            val serverFactory = NettyReactiveWebServerFactory()
            serverFactory.setResourceFactory(resourceFactory)
            routes.orderedStream().forEach { routeProviders: NettyRouteProvider? ->
                serverFactory.addRouteProviders(
                    routeProviders
                )
            }

            return serverFactory
        }
    }

}