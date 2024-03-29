import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    targets {
        jvm()
        linuxX64()
        mingwX64().apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        macosX64()
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("io.ktor.util.InternalAPI")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation(project(":logging"))
                implementation(project(":common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.eclipse.jetty.websocket:javax-websocket-client-impl:9.4.51.v20230217")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation(project(":interceptor-http"))
            }
        }
        val posixMain by creating {
            dependsOn(nativeMain)
        }
        val linuxX64Main by getting {
            dependsOn(posixMain)
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(posixMain)
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
