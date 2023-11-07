import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.interceptor"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val nativePreset = presets[HostManager.host.presetName] as AbstractKotlinNativeTargetPreset
    val nativeAgentLibName = "hook"
    val nativeAgentTargetName = "nativeAgent"
    targets {
        jvm()
        jvm("jvmSunServer")
        jvm("jvmUndertowServer")
        jvm("jvmJettyServer")
        targetFromPreset(nativePreset, nativeAgentTargetName) {
            binaries.sharedLib(nativeAgentLibName, setOf(DEBUG)) {
                if(HostManager.hostIsMingw) {
                    linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
                }
            }
        }
        targets.withType<KotlinJvmTarget> {
            compilations["main"].defaultSourceSet.dependencies {
                implementation(kotlin("reflect"))
            }
            compilations["test"].defaultSourceSet.dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jvmSunServerTest by getting {
            dependsOn(jvmMain)
        }
        val jvmUndertowServerTest by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation("io.undertow:undertow-core:2.0.29.Final")
                implementation("io.undertow:undertow-servlet:2.0.29.Final")
            }
        }
        val jvmJettyServerTest by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation("org.eclipse.jetty:jetty-server:9.4.26.+")
            }
        }
        val nativeAgentMain by getting {
            dependencies {
                if(HostManager.hostIsMingw) implementation(project(":logging-native"))
                implementation(project(":logging"))
                implementation(project(":jvmapi"))
                implementation(project(":drill-hook"))
                api(project(":interceptor-http"))
            }
        }
    }
    val filterOutDefaultJvm: (KotlinJvmTest) -> Boolean = {
        it.targetName != "jvm"
    }
    val nativeAgentLinkTask = tasks["link${nativeAgentLibName.capitalize()}DebugShared${nativeAgentTargetName.capitalize()}"]
    val nativeAgentTarget = targets.withType<KotlinNativeTarget>().getByName(nativeAgentTargetName)
    val nativeAgentFile = nativeAgentTarget.binaries.findSharedLib(nativeAgentLibName, NativeBuildType.DEBUG)!!.outputFile
    tasks {
        withType<KotlinJvmTest>().filter(filterOutDefaultJvm).forEach {
            it.dependsOn(nativeAgentLinkTask)
            it.testLogging.showStandardStreams = true
            it.jvmArgs("-agentpath:${nativeAgentFile.toPath()}")
        }
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
}
