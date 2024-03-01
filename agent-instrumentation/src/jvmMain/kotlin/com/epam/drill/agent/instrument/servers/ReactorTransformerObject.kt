package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import javassist.CtClass
import mu.KotlinLogging

const val FLUX_CLASS_NAME = "reactor/core/publisher/Flux"
const val MONO_CLASS_NAME = "reactor/core/publisher/Mono"
const val PARALLEL_FLUX_CLASS_NAME = "reactor/core/publisher/ParallelFlux"
const val SCHEDULERS_CLASS_NAME = "reactor/core/scheduler/Schedulers"

abstract class ReactorTransformerObject(
    private val reactorTransformers: Set<AbstractTransformerObject>
) : AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        reactorTransformers.any { it.permit(className, null, null) }

    override fun transform(className: String, ctClass: CtClass) {
        reactorTransformers.find { it.permit(className, null, null) }
            ?.transform(className, ctClass)
            ?: logger.error { "Reactor transformer object for class $className not found" }
    }
}