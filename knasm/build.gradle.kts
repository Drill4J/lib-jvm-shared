import java.util.Properties
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
}

group = "com.epam.drill.knasm"
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
        mingwX64()
        linuxX64()
        macosX64()
        macosArm64()
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.5.2")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    tasks {
        val jvmTest by getting(KotlinJvmTest::class) {
            useJUnitPlatform()
        }
    }
}
