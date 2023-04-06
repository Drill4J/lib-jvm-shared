package de.nielsfalk.ktor.swagger

import de.nielsfalk.ktor.swagger.version.shared.CommonBase
import de.nielsfalk.ktor.swagger.version.shared.Group
import de.nielsfalk.ktor.swagger.version.shared.ModelName
import de.nielsfalk.ktor.swagger.version.shared.OperationBase
import de.nielsfalk.ktor.swagger.version.shared.ParameterBase
import de.nielsfalk.ktor.swagger.version.shared.ParameterInputType
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import de.nielsfalk.ktor.swagger.version.v3.OpenApi
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.reflect.*
import java.util.*
import io.ktor.util.reflect.typeInfo
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import de.nielsfalk.ktor.swagger.version.v2.Operation as OperationV2
import de.nielsfalk.ktor.swagger.version.v2.Parameter as ParameterV2
import de.nielsfalk.ktor.swagger.version.v2.Response as ResponseV2
import de.nielsfalk.ktor.swagger.version.v3.Operation as OperationV3
import de.nielsfalk.ktor.swagger.version.v3.Parameter as ParameterV3
import de.nielsfalk.ktor.swagger.version.v3.Response as ResponseV3

class SwaggerSupport(
    val swagger: Swagger?,
    val swaggerCustomization: Metadata.(HttpMethod) -> Metadata,
    val openApi: OpenApi?,
    val openApiCustomization: Metadata.(HttpMethod) -> Metadata,
) {
    companion object Feature : ApplicationFeature<Application, SwaggerUiConfiguration, SwaggerSupport> {
        private val openApiJsonFileName = "openapi.json"
        private val swaggerJsonFileName = "swagger.json"

        internal val swaggerVariation = SpecVariation("#/definitions/", ResponseV2, OperationV2, ParameterV2)
        internal val openApiVariation = SpecVariation("#/components/schemas/", ResponseV3, OperationV3, ParameterV3)

        override val key = AttributeKey<SwaggerSupport>("SwaggerSupport")

        override fun install(pipeline: Application, configure: SwaggerUiConfiguration.() -> Unit): SwaggerSupport {
            val (
                path,
                forwardRoot,
                provideUi,
                swagger,
                openApi,
                swaggerConfig,
                openpapiConfig,
            ) = SwaggerUiConfiguration().apply(configure)
            val feature = SwaggerSupport(swagger, swaggerConfig, openApi, openpapiConfig)

            val defaultJsonFile = when {
                openApi != null -> openApiJsonFileName
                swagger != null -> swaggerJsonFileName
                else -> throw IllegalArgumentException("Swagger or OpenApi must be specified")
            }
            pipeline.routing {
                get("/$path") {
                    redirect(path, defaultJsonFile)
                }
                val ui = if (provideUi) SwaggerUi() else null
                get("/$path/{fileName}") {
                    val filename = call.parameters["fileName"]
                    if (filename == swaggerJsonFileName && swagger != null) {
                        call.respond(swagger.toJsonContent())
                    } else if (filename == openApiJsonFileName && openApi != null) {
                        call.respond(openApi.toJsonContent())
                    } else {
                        ui?.serve(filename, call)
                    }
                }
                if (forwardRoot) {

                    get("/") {
                        redirect(path, defaultJsonFile)
                    }
                }
            }
            return feature
        }

        private suspend fun PipelineContext<Unit, ApplicationCall>.redirect(path: String, defaultJsonFile: String) {
            call.respondRedirect("/$path/index.html?url=./$defaultJsonFile")
        }
    }

    val commons: Collection<CommonBase> =
        listOf(swagger, openApi).filterNotNull()

    private val variations: Collection<BaseWithVariation<out CommonBase>>
        get() = commons.map {
            when (it) {
                is Swagger -> SwaggerBaseWithVariation(
                    it,
                    swaggerCustomization,
                    swaggerVariation
                )
                is OpenApi -> OpenApiBaseWithVariation(
                    it,
                    openApiCustomization,
                    openApiVariation
                )
                else -> throw IllegalStateException("Must be of type ${Swagger::class.simpleName} or ${OpenApi::class.simpleName}")
            }
        }

    inline fun <reified LOCATION : Any, reified ENTITY_TYPE : Any> Metadata.apply(method: HttpMethod) {
        apply(LOCATION::class, typeInfo<ENTITY_TYPE>(), method)
    }

    @PublishedApi
    internal fun Metadata.apply(locationClass: KClass<*>, bodyTypeInfo: TypeInfo, method: HttpMethod) {
        variations.forEach {
            it.apply { metaDataConfiguration(method).apply(locationClass, bodyTypeInfo, method) }
        }
    }
}

private class SwaggerBaseWithVariation(
    base: Swagger,
    metaDataConfiguration: Metadata.(HttpMethod) -> Metadata,
    variation: SpecVariation,
) : BaseWithVariation<Swagger>(base, metaDataConfiguration, variation) {

    override val schemaHolder: MutableMap<ModelName, Any>
        get() = base.definitions

    override fun addDefinition(name: String, schema: Any) {
        base.definitions.putIfAbsent(name, schema)
    }
}

private class OpenApiBaseWithVariation(
    base: OpenApi,
    metaDataConfiguration: Metadata.(HttpMethod) -> Metadata,
    variation: SpecVariation,
) : BaseWithVariation<OpenApi>(base, metaDataConfiguration, variation) {
    override val schemaHolder: MutableMap<ModelName, Any>
        get() = base.components.schemas

    override fun addDefinition(name: String, schema: Any) {
        base.components.schemas.putIfAbsent(name, schema)
    }
}

//TODO EPMDJ-8530 clear the collection after all endpoints are registered
private val ignoredProperties: MutableMap<KClass<*>, Set<String>> = mutableMapOf()

private abstract class BaseWithVariation<B : CommonBase>(
    val base: B,
    val metaDataConfiguration: Metadata.(HttpMethod) -> Metadata,
    val variation: SpecVariation,
) {
    abstract val schemaHolder: MutableMap<ModelName, Any>

    abstract fun addDefinition(name: String, schema: Any)

    fun addDefinition(typeInfo: TypeInfo) {
        if (typeInfo.type != Unit::class) {
            val accruedNewDefinitions = mutableListOf<TypeInfo>()
            schemaHolder
                .computeIfAbsent(typeInfo.modelName()) {
                    val modelWithAdditionalDefinitions = variation {
                        createModelData(typeInfo)
                    }
                    accruedNewDefinitions.addAll(modelWithAdditionalDefinitions.second)
                    modelWithAdditionalDefinitions.first
                }

            accruedNewDefinitions.forEach { addDefinition(it) }
        }
    }

    fun addDefinitions(
        kClasses: Collection<TypeInfo>,
    ) = kClasses.forEach {
        addDefinition(it)
    }

    private fun KClass<*>.addIgnoreProp(ignoredPropertiesNames: Array<String>) {
        takeIf { ignoredPropertiesNames.isNotEmpty() }?.also { kClass ->
            ignoredProperties[kClass]?.let { arr ->
                ignoredProperties[kClass] = (ignoredPropertiesNames + arr).toSet()
            } ?: run {
                ignoredProperties[kClass] = ignoredPropertiesNames.toSet()
            }
            nestedClasses.forEach {
                it.addIgnoreProp(ignoredPropertiesNames)
            }
        }
    }

    fun <LOCATION : Any> Metadata.applyOperations(
        location: Location,
        group: Group?,
        method: HttpMethod,
        locationType: KClass<LOCATION>,
        bodyType: BodyType,
    ) {
        locationType.run {
            findAnnotation<Ignore>()?.let {
                locationType.addIgnoreProp(it.properties)
            }
            //Hack to get a parent class from nested class when parent class in param of nested class
            memberProperties.forEach {
                (it.returnType.classifier as? KClass<*>)?.let {
                    it.findAnnotation<Ignore>()?.run {
                        it.addIgnoreProp(properties)
                    }
                }
            }
        }

        if (bodyType is BodyFromReflection && bodyType.typeInfo.type != Unit::class) {
            addDefinition(bodyType.typeInfo)
        }

        fun createOperation(): OperationBase {
            val responses = responses.map { codeResponse ->
                codeResponse.responseTypes.forEach {
                    if (it is JsonResponseFromReflection) {
                        addDefinition(it.type)
                    }
                }

                val response = variation.reponseCreator.create(codeResponse)

                codeResponse.statusCode.value.toString() to response
            }.toMap().filterNullValues()

            val parameters = mutableListOf<ParameterBase>().apply {
                variation {
                    if ((bodyType as? BodyFromReflection)?.typeInfo?.type != Unit::class) {
                        add(bodyType.bodyParameter())
                    }
                    addAll(locationType.memberProperties.mapNotNull {
                        if (it.findAnnotation<Ignore>() != null || ignoredProperties[locationType]?.contains(it.name) == true) {
                            return@mapNotNull null
                        }
                        it.toParameter(location.path).let {
                            addDefinitions(it.second)
                            it.first
                        }
                    })
                    fun KClass<*>.processToParameters(parameterType: ParameterInputType) {
                        addAll(memberProperties.map {
                            it.toParameter(location.path, parameterType).let {
                                addDefinitions(it.second)
                                it.first
                            }
                        })
                    }
                    parameters.forEach { it.processToParameters(ParameterInputType.query) }
                    headers.forEach { it.processToParameters(ParameterInputType.header) }
                }
            }

            return variation.operationCreator.create(
                this,
                responses,
                parameters,
                location,
                group,
                method,
                bodyExamples,
                operationId
            )
        }

        base.paths
            .getOrPut(location.path) { mutableMapOf() }
            .put(
                method.value.lowercase(Locale.getDefault()),
                createOperation()
            )
    }

    private fun <K : Any, V> Map<K, V?>.filterNullValues(): Map<K, V> {
        val destination = mutableListOf<Pair<K, V>>()
        forEach {
            val valueSaved = it.value
            if (valueSaved != null) {
                destination.add(it.key to valueSaved)
            }
        }
        return destination.toMap()
    }

    private fun Metadata.createBodyType(typeInfo: TypeInfo): BodyType = when {
        bodySchema != null -> {
            BodyFromSchema(
                name = bodySchema.name ?: typeInfo.modelName(),
                examples = bodyExamples
            )
        }
        typeInfo.type == String::class -> BodyFromString(bodyExamples)
        else -> BodyFromReflection(typeInfo, bodyExamples)
    }

    private fun Metadata.requireMethodSupportsBody(method: HttpMethod) =
        require(!(methodForbidsBody.contains(method) && bodySchema != null)) {
            "Method type $method does not support a body parameter."
        }

    internal fun Metadata.apply(locationClass: KClass<*>, bodyTypeInfo: TypeInfo, method: HttpMethod) {
        requireMethodSupportsBody(method)
        val bodyType = createBodyType(bodyTypeInfo)
        val clazz = locationClass.java
        val location = locationClass.toLocation()
        val tags = clazz.getAnnotation(Group::class.java)

        applyOperations(location, tags, method, locationClass, bodyType)
    }

    companion object {
        /**
         * The [HttpMethod] types that don't support having a HTTP body element.
         */
        private val methodForbidsBody = setOf(HttpMethod.Get, HttpMethod.Delete)
    }
}

data class SwaggerUiConfiguration(
    var path: String = "apidocs",
    var forwardRoot: Boolean = false,
    var provideUi: Boolean = true,
    var swagger: Swagger? = null,
    var openApi: OpenApi? = null,
    /**
     * Customization mutation applied to every [Metadata] processed for the swagger.json
     */
    var swaggerCustomization: Metadata.(HttpMethod) -> Metadata = { this },
    /**
     * Customization mutation applied to every [Metadata] processed for the openapi.json
     */
    var openApiCustomization: Metadata.(HttpMethod) -> Metadata = { this },
)
