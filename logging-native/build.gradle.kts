import java.util.Properties

plugins {
    kotlin("multiplatform")
}

group = "com.epam.drill"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}
val macosLd64: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    mingwX64()
    macosX64().apply {
        if(macosLd64.toBoolean()){
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    macosArm64().apply {
        if (macosLd64.toBoolean()) {
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
