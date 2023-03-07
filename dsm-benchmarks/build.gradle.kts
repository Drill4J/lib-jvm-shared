import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    id("org.jetbrains.kotlinx.benchmark")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.dsm"

val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val kotlinxBenchmarkVersion: String by parent!!.extra
val exposedVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.Experimental")
    languageSettings.optIn("kotlin.time.ExperimentalTime")
    languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    languageSettings.optIn("kotlinx.serialization.ImplicitReflectionSerializer")
    languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$kotlinxBenchmarkVersion")
    implementation(project(":dsm"))
    api(project(":dsm-annotations"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    testImplementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    testImplementation(project(":dsm-test-framework"))
}

tasks {
    test {
        useJUnitPlatform()
        systemProperty("plugin.feature.drealtime", false)
    }
    withType(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

noArg {
    annotation("kotlinx.serialization.Serializable")
}

benchmark {
    configurations.getByName("main") {
        iterationTime = 5
        iterationTimeUnit = "ms"
    }
    targets.register("test") {
        (this as JvmBenchmarkTarget).jmhVersion = "1.21"
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
