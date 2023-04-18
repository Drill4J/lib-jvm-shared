import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
    id("com.epam.drill.gradle.plugin.kni")
}

group = "com.epam.drill.logger"
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
    targets {
        val jvm = jvm()
        val native = targetFromPreset(nativePreset, "native") {
            binaries.sharedLib("testAgent", setOf(DEBUG))
        }
        kni {
            nativeCrossCompileTarget = sequenceOf(native)
            jvmTargets = sequenceOf(jvm)
            jvmtiAgentObjectPath = "com.epam.drill.test.Agent"
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":kni-runtime"))
                implementation(project(":logger"))
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(project(":kni-runtime"))
                implementation(project(":jvmapi"))
                implementation(project(":logger"))
            }
        }
    }
    tasks {
        val generateNativeClasses by getting
        val jvmProcessResources by getting
        val compileKotlinNative by getting
        jvmProcessResources.dependsOn(generateNativeClasses)
        compileKotlinNative.dependsOn(generateNativeClasses)
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            delete("src/jvmMain/resources/kni-meta-info")
            delete("src/nativeMain/kotlin/kni")
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
