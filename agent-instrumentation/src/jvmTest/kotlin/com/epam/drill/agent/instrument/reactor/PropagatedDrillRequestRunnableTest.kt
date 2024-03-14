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
package com.epam.drill.agent.instrument.reactor

import kotlin.test.BeforeTest
import kotlin.test.Test
import java.time.Duration
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import com.epam.drill.common.agent.request.DrillRequest

class PropagatedDrillRequestRunnableTest {

    @BeforeTest
    fun setUp() {
        //Reset all installed hooks
        Hooks.resetOnEachOperator()
        Schedulers.resetOnScheduleHooks()

        // InheritableThreadLocal / TransmittableThreadLocal in RequestHolder
        // enable propagating context to child threads
        // Warm-up creates child thread in advance, when no drill context is present in thread local yet
        // later this thread w/o initial context is re-used when calling Schedulers.single()
        // it won't work in Schedulers.parallel()
        Mono.just("warm-up")
            .subscribeOn(Schedulers.single())
            .block()
    }

    @Test
    fun `given MonoDelay class, PropagatedDrillRequestRunnable should seamlessly propagate test session to scheduled task`() {
        val testSession = "test-session"

        //Use Reactor Schedulers Hook to replace the Runnable class with a decorator one
        Schedulers.onScheduleHook("test-hook") { runnable ->
            TestRequestHolder.retrieve()?.let { PropagatedDrillRequestRunnable(it, TestRequestHolder, runnable) } ?: runnable
        }

        //Save testSession in the current thread
        TestRequestHolder.store(DrillRequest(testSession))
        try {
            val mono = Mono.just("task")
                .delaySubscription(Duration.ofMillis(10), Schedulers.single())
                .map {
                    //Retrieve testSession in a scheduled task
                    "$it-${TestRequestHolder.retrieve()?.drillSessionId}"
                }

            StepVerifier.create(mono)
                //Check that testSession was propagated to subscriber thread
                .expectNext("task-$testSession")
                .expectComplete()
                .verify()
        } finally {
            TestRequestHolder.remove()
        }
    }

}