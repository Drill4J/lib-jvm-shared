import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val javassistVersion: String by parent!!.extra
val transmittableThreadLocalVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    targets {
        jvm()
        mingwX64()
        linuxX64()
        macosX64()
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName].compilations.forEach {
            it.defaultSourceSet.kotlin.srcDir("src/native${it.compilationName.capitalize()}/kotlin")
            it.defaultSourceSet.resources.srcDir("src/native${it.compilationName.capitalize()}/resources")
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":logging"))
                implementation(project(":common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.javassist:javassist:$javassistVersion")
                implementation("com.alibaba:transmittable-thread-local:$transmittableThreadLocalVersion")
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation(project(":jvmapi"))
            }
        }
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
    }
    tasks {
        val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
            it.targetName != HostManager.host.presetName
        }
        val copyNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "copyNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val copyNativeClasses: TaskProvider<Copy> = register(taskName, Copy::class) {
                group = "build"
                from("src/native${it.compilationName.capitalize()}/kotlin")
                into("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            it.compileKotlinTask.dependsOn(copyNativeClasses.get())
        }
        val cleanNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "cleanNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val cleanNativeClasses: TaskProvider<Delete> = register(taskName, Delete::class) {
                group = "build"
                delete("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            clean.get().dependsOn(cleanNativeClasses.get())
        }
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform)
            .flatMap(KotlinNativeTarget::compilations)
            .onEach(copyNativeClassesTask)
            .onEach(cleanNativeClassesTask)
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni")
        }
    }
}
