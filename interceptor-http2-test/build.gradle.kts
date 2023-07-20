import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
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
    @Suppress("DEPRECATION")
    jcenter()
}

kotlin {
    val nativePreset = presets[HostManager.host.presetName] as AbstractKotlinNativeTargetPreset
    val nativeAgentLibName = "hook"
    val nativeAgentTargetName = "nativeAgent"
    targets {
        jvm()
        //jvm("jvmJettyServer")
        targetFromPreset(nativePreset, nativeAgentTargetName) {
            binaries.sharedLib(nativeAgentLibName, setOf(DEBUG)) {
                if(HostManager.hostIsMingw) {
                    linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
                }
            }
        }
    }
    //val tcnativeClassifier = when(HostManager.host.presetName) {
    //    "linuxX64" -> "linux-x86_64"
    //    "mingwX64" -> "windows-x86_64"
    //    "macosX64" -> "osx-x86_64"
    //    else -> ""
    //}
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
        val commonMain by getting
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(kotlin("test-junit"))
            }
        }
        //val jvmJettyServerTest by getting {
        //    dependsOn(jvmMain)
        //    dependencies {
        //        implementation("io.ktor:ktor-server-netty:1.3.2")
        //        implementation("io.ktor:ktor-html-builder:1.3.2")
        //        implementation("io.ktor:ktor-network-tls:1.3.2")
        //        implementation("io.ktor:ktor-network-tls-certificates:1.3.2")
        //        implementation("io.ktor:ktor-client-jetty:1.3.2")
        //        implementation("io.netty:netty-tcnative:2.0.28.Final")
        //        implementation("io.netty:netty-tcnative-boringssl-static:2.0.28.Final")
        //        implementation("io.netty:netty-tcnative-boringssl-static:2.0.28.Final:$tcnativeClassifier")
        //        implementation("org.eclipse.jetty.http2:http2-client")
        //    }
        //}
        val nativeAgentMain by getting {
            dependsOn(commonMain)
            dependencies {
                if(HostManager.hostIsMingw) implementation(project(":logging-native"))
                implementation(project(":logging"))
                implementation(project(":jvmapi"))
                api(project(":interceptor-http2"))
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
