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
package com.epam.drill.kni.gradle

import com.epam.drill.kni.*
import org.apache.bcel.classfile.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.internal.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.konan.properties.*
import java.util.jar.*

private const val EXTENSION_NAME = "kni"

val allowedAnnotations = setOf(Kni::class).map { "L${it.java.canonicalName.jvmfy()};" }

private const val kniMetaInfoFileName = "kni-meta-info"

@Suppress("unused")
open class KniPlugin : Plugin<Project> {

    lateinit var kotlinExtDsl: DslObject


    override fun apply(target: Project) = target.run {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExt = extensions.getByName("kotlin") as KotlinProjectExtension
            kotlinExtDsl = DslObject(kotlinExt)

            val generateNativeClasses = project.task("generateNativeClasses") {
                it.group = "kni"
                it.doLast {
                    val config = config()
                    config.nativeCrossCompileTarget.forEach { target ->
                        val outputDir = file("src/${target.name}Main/kotlin/kni")
                        val gen = Generator(outputDir)
                        config.additionalJavaClasses.forEach { className ->
                            runCatching {
                                val cls = Class.forName(className)
                                gen.generate(
                                    ClassParser(
                                        cls.getResourceAsStream("/" + className.jvmfy().suffix("class")),
                                        cls.name
                                    ).parse()
                                )
                            }.onFailure {
                                println("Error while generation stub for $className ${it.message} ")
                            }
                        }

                        config.jvmtiAgentObjectPath?.let {
                            JvmtiGenerator.generate(it, outputDir)
                        }
                        config.jvmTargets.removeKniMetaInfo()
                        config.jvmTargets.forEach {
                            val excludedClasses = config.excludedClasses.map {
                                it.jvmfy().suffix("class")
                            }.toMutableSet()
                            val map = it.mainCompilation.compileDependencyFiles
                                .filter { it.extension == "jar" }
                                .map {
                                    JarFile(it).use { jar ->
                                        excludedClasses.addAll(jar.entries().asSequence().filter {
                                            !it.isDirectory && it.name == kniMetaInfoFileName
                                        }.flatMap {
                                            jar.getInputStream(it).readBytes().decodeToString().split(",")
                                        })
                                        jar.entries().asSequence().toList()
                                            .filter {
                                                !it.isDirectory && it.name.contains(".class")
                                                        && !excludedClasses.contains(it.name)
                                            }.map { jar.getInputStream(it).readBytes() }
                                    }
                                }.flatten() + it.mainCompilation.compileKotlinTask.outputs.files.singleFile
                                .walkTopDown()
                                .filter { it.isFile && it.extension == "class" }
                                .map { it.inputStream().readBytes() }
                            val classesToExcluded = mutableSetOf<String>()
                            map.forEach {
                                val jvmClass = ClassParser(it.inputStream(), "").parse()
                                if (jvmClass.annotationEntries.any { allowedAnnotations.contains(it.annotationType) }) {
                                    println("Kni class candidate: ${jvmClass.className}")
                                    gen.generate(jvmClass)
                                    classesToExcluded.add("${jvmClass.className.jvmfy()}.class")
                                }
                            }
                            it.mainCompilation.defaultSourceSet.resources.takeIf {
                                classesToExcluded.isNotEmpty()
                            }?.addKniMetaInfo(classesToExcluded)
                        }
                    }
                }
            }
            @Suppress("UNCHECKED_CAST") val targets =
                kotlinExtDsl.extensions.getByName("targets") as NamedDomainObjectCollection<KotlinTarget>
            targets.jvmTargets().all {
                val compileKotlinTask = it.mainCompilation.compileKotlinTask
                generateNativeClasses.dependsOn(compileKotlinTask)
            }

            kotlinExtDsl.extensions.create(EXTENSION_NAME, KniExt::class.java, targets)
        }

    }

    private fun config() = kotlinExtDsl.extensions.findByName(EXTENSION_NAME) as KniExt

}

fun NamedDomainObjectCollection<KotlinTarget>.jvmTargets(
    matcher: KotlinJvmTarget.() -> Boolean = { true },
) = withType(KotlinJvmTarget::class.java).matching(matcher)

val KotlinTarget.mainCompilation: KotlinCompilation<*>
    get() = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

private fun Sequence<KotlinJvmTarget>.removeKniMetaInfo() = forEach {
    it.mainCompilation.defaultSourceSet.resources.srcDirs.first().resolve(kniMetaInfoFileName).delete()
}

private fun SourceDirectorySet.addKniMetaInfo(classesToExcluded: Set<String>) {
    srcDirs.first().apply { mkdirs() }.resolve(kniMetaInfoFileName).apply {
        createNewFile()
        writeText(classesToExcluded.joinToString(","))
    }
}

open class KniExt(
    private val targets: NamedDomainObjectCollection<KotlinTarget>,
) {
    /**
     * List classes without [Kni] annotation, but for which you need to generate a stub
     * Example : sequenceOf("com.epam.drill.TestClass")
     */
    var additionalJavaClasses: Sequence<String> = sequenceOf()

    /**
     * List of jvm targets where we search classes with [Kni] annotation
     */
    var jvmTargets: Sequence<KotlinJvmTarget> = sequenceOf()

    /**
     * List of native targets for which stub classes will be generated
     */
    var nativeCrossCompileTarget: Sequence<KotlinNativeTarget> = sequenceOf()

    /**
     * Path to the class implemented [JvmtiAgent] interface
     * Example "com.epam.drill.core.Agent"
     */
    var jvmtiAgentObjectPath: String? = null

    /**
     * List of class for witch stub won't be generated
     * Example : sequenceOf("com.epam.drill.logger.NativeApi")
     */
    var excludedClasses: Sequence<String> = sequenceOf()
}
