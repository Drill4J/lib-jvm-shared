import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

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
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                kotlin("stdlib")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
