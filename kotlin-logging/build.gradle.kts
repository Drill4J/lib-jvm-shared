import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "io.github.microutils"
version = "2.1.24"

repositories {
    mavenCentral()
}

val logbackVersion: String by parent!!.extra
val junitVersion: String by parent!!.extra
val mockitoVersion: String by parent!!.extra

kotlin {
    explicitApi()
    jvm {
        compilations.all {
            // kotlin compiler compatibility options
            kotlinOptions {
                apiVersion = "1.9"
                languageVersion = "1.9"
                jvmTarget = "1.8"
            }
        }
    }

    val linuxTargets = listOf(
        linuxArm64(),
        linuxX64(),
        mingwX64(),
        macosX64(),
        macosArm64()
    )

    sourceSets {
        val commonMain by getting {}
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                implementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
                implementation("org.mockito:mockito-all:$mockitoVersion")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val linuxMain by creating {
            dependsOn(nativeMain)
        }
        linuxTargets.forEach {
            getByName("${it.targetName}Main") {
                dependsOn(linuxMain)
            }
        }
    }
}

tasks {

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = FULL
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
