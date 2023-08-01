import java.net.URI
import java.util.Properties
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

val ktorVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    targets {
        linuxX64()
        mingwX64()
        macosX64()
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting
        val commonNative by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation(project(":drill-hook"))
            }
        }
        val linuxX64Main by getting {
            dependsOn(commonNative)
        }
        val mingwX64Main by getting {
            dependsOn(commonNative)
        }
        val macosX64Main by getting {
            dependsOn(commonNative)
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
