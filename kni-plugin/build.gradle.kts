import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.gradle.plugin"

val kotlinVersion: String by parent!!.extra
val kotlinPoetVersion: String by parent!!.extra
val bcelVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("kni") {
            id = "$group.kni"
            implementationClass = "com.epam.drill.kni.gradle.KniPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation(gradleApi())
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("org.apache.bcel:bcel:$bcelVersion")
    implementation(project(":kni-runtime"))
    testImplementation(kotlin("test", kotlinVersion))
    testImplementation(kotlin("test-junit", kotlinVersion))
    testImplementation(gradleTestKit())
}


@Suppress("UNUSED_VARIABLE")
tasks {
    val compileKotlin by getting(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
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
