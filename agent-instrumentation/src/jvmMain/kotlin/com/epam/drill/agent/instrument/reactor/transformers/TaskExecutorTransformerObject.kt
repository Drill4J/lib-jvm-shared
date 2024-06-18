package com.epam.drill.agent.instrument.reactor.transformers

import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.reactor.PropagatedDrillRequestCallable
import com.epam.drill.agent.instrument.reactor.PropagatedDrillRequestRunnable
import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.common.agent.request.RequestHolder

abstract class TaskExecutorTransformerObject : RequestHolder, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        listOf(
            "org/springframework/core/task/SimpleAsyncTaskExecutor",
            "org/springframework/scheduling/concurrent/ConcurrentTaskExecutor",
            "org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting TaskExecutorTransformer for $className..." }
        ctClass.getMethod("submitListenable", "(Ljava/util/concurrent/Callable;)Lorg/springframework/util/concurrent/ListenableFuture;")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                if (drillRequest != null) $1 = new ${PropagatedDrillRequestCallable::class.java.name}(drillRequest, ${this::class.java.name}.INSTANCE, $1);
                """.trimIndent()
            )
        ctClass.getMethod("submitListenable", "(Ljava/lang/Runnable;)Lorg/springframework/util/concurrent/ListenableFuture;")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                if (drillRequest != null) $1 = new ${PropagatedDrillRequestRunnable::class.java.name}(drillRequest, ${this::class.java.name}.INSTANCE, $1);
                """.trimIndent()
            )
    }

}
