import java.net.URI
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.logger"

val ktorVersion: String by parent!!.extra
val loggerSkipJvmTests: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    targets {
        jvm()
        linuxX64()
        mingwX64()
        macosX64()
        macosArm64()
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting {
            dependencies {
                api(project(":logger-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":kni-runtime"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-utils:$ktorVersion")
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosArm64Main by getting {
            dependsOn(commonMain)
        }
    }
    @Suppress("UNUSED_VARIABLE")
    tasks {
        val linkAgentTask = getByPath(":logger-test-agent:linkTestAgentDebugSharedNative")
        val jvmTest by getting(KotlinJvmTest::class) {
            enabled = !loggerSkipJvmTests.toBoolean()
            doFirst {
                val extensions = setOf("so", "dylib", "dll")
                val agentLib = linkAgentTask.outputs.files.first().walkTopDown().last { it.extension in extensions }
                jvmArgs("-agentpath:$agentLib")
            }
        }
        jvmTest.dependsOn(linkAgentTask)
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
