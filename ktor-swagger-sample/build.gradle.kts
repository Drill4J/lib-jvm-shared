import java.util.Properties

@Suppress("RemoveRedundantBackticks")
plugins {
    `java-library`
    `application`
    `jacoco`
    kotlin("jvm")
}

group = "com.epam.drill.ktor"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val ktorVersion: String by parent!!.extra
val ajaltCliktVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("com.github.ajalt.clikt:clikt:$ajaltCliktVersion")
    implementation(project(":ktor-swagger"))

    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    api("io.ktor:ktor-locations:$ktorVersion")
    api("io.ktor:ktor-server-core:$ktorVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-gson:$ktorVersion")
    testImplementation("com.winterbe:expekt:0.5.0")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.ExperimentalStdlibApi")
    languageSettings.optIn("io.ktor.locations.KtorExperimentalLocationsAPI")
}

application {
    mainClass.set("de.nielsfalk.ktor.swagger.sample.JsonApplicationKt")
}

jacoco {
    toolVersion = "0.8.2"
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}
