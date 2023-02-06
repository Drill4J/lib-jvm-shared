import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    `java-gradle-plugin`
    `kotlin-dsl-base`
    id("com.github.hierynomus.license")
}

group = "com.epam.drill"

val kotlinVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("cross-compilation") {
            id = "$group.cross-compilation"
            implementationClass = "com.epam.drill.gradle.CrossCompilation"
        }
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin", kotlinVersion))
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
