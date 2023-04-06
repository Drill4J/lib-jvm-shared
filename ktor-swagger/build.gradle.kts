@Suppress("RemoveRedundantBackticks")
plugins {
    `java-library`
    `jacoco`
    kotlin("jvm")
}

group = "com.epam.drill.ktor"

val ktorVersion: String by parent!!.extra
val webjarsSwaggerUiVersion: String by parent!!.extra
val googleGsonVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    api("io.ktor:ktor-locations:$ktorVersion")
    api("io.ktor:ktor-server-core:$ktorVersion")

    /*
     * Webjars have their resources pakaged in a version specific directory.
     * When this version is bumped, the version in the `SwaggerUi` where the resouce
     * is loaded must also be bumped.
     */
    implementation("org.webjars:swagger-ui:$webjarsSwaggerUiVersion")
    implementation("com.google.code.gson:gson:$googleGsonVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-gson:$ktorVersion")
    testImplementation("com.winterbe:expekt:0.5.0")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.ExperimentalStdlibApi")
    languageSettings.optIn("io.ktor.locations.KtorExperimentalLocationsAPI")
}

jacoco {
    toolVersion = "0.8.2"
}

@Suppress("UNUSED_VARIABLE")
tasks {
    jacocoTestReport {
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }
    val sourcesJar by registering(Jar::class) {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}
