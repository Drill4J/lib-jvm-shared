import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.interceptor"

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
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting
        val commonNative by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":drill-hook"))
                implementation(project(":logger"))
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
