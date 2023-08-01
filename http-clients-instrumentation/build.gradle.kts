import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
    id("com.epam.drill.gradle.plugin.kni")
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val javassistVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val currentPlatformTarget: KotlinMultiplatformExtension.() -> KotlinNativeTarget = {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
    }
    targets {
        val jvm = jvm()
        val mingwX64 = mingwX64()
        val linuxX64 = linuxX64()
        val macosX64 = macosX64()
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
        }
        kni {
            jvmTargets = sequenceOf(jvm)
            additionalJavaClasses = sequenceOf()
            nativeCrossCompileTarget = sequenceOf(mingwX64, linuxX64, macosX64)
            excludedClasses = sequenceOf("com.epam.drill.logger.NativeApi")
        }

    }
    val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
        dependencies {
            implementation(project(":kni-runtime"))
            implementation(project(":knasm"))
            implementation(project(":jvmapi"))
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":knasm"))
                implementation(project(":logging"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.javassist:javassist:$javassistVersion")
                implementation(project(":kni-runtime"))
                implementation(project(":knasm"))
            }
        }
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
    }
    val copyNativeClassesForTarget: TaskContainer.(KotlinNativeTarget) -> Task = {
        val copyNativeClasses:TaskProvider<Copy> = register("copyNativeClasses${it.targetName.capitalize()}", Copy::class) {
            group = "build"
            from("src/nativeMain/kotlin")
            into("src/${it.targetName}Main/kotlin/gen")
        }
        copyNativeClasses.get()
    }
    val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
        it.targetName != HostManager.host.presetName
    }
    tasks {
        val generateNativeClasses by getting
        val jvmProcessResources by getting
        jvmProcessResources.dependsOn(generateNativeClasses)
        currentPlatformTarget().compilations["main"].compileKotlinTask.dependsOn(generateNativeClasses)
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            copyNativeClasses.dependsOn(generateNativeClasses)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
        }
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            delete("src/jvmMain/resources/kni-meta-info")
            delete("src/nativeMain/kotlin/kni")
            kotlin.targets.withType<KotlinNativeTarget> {
                delete("src/${name}Main/kotlin/kni")
                delete("src/${name}Main/kotlin/gen")
            }
        }
        clean.dependsOn(cleanGeneratedClasses)
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
