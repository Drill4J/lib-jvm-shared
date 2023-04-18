import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `application`
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.ts"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    target.compilations.all {
        kotlinOptions.jvmTarget = "${JavaVersion.VERSION_1_8}"
    }
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
    }
}

dependencies {
    implementation("com.github.ajalt:clikt:2.7.1")
    implementation(project(":kt2dts"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

val jarMainClassName = "com.epam.drill.ts.kt2dts.cli.MainKt"

@Suppress("UNUSED_VARIABLE")
tasks {
    val sourcesJar by registering(Jar::class) {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    val fatJar by registering(Jar::class) {
        archiveBaseName.set("${project.name}-runtime")
        manifest.attributes["Main-Class"] = jarMainClassName
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(
            sourceSets.main.get().output,
            configurations.runtimeClasspath.get().resolve().map(::zipTree)
        )
    }
    test.get().dependsOn(project(":kt2dts-api-sample").tasks.jar)
}

application {
    mainClass.set(jarMainClassName)
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
