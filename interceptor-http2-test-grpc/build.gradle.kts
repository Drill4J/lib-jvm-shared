import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
    id("com.google.protobuf")
}

group = "com.epam.drill.interceptor"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

@Suppress("GradlePackageVersionRange")
dependencies {
    compileOnly("javax.annotation:javax.annotation-api:1.2")
    implementation("io.grpc:grpc-netty-shaded:+")
    implementation("io.grpc:grpc-protobuf:+")
    implementation("io.grpc:grpc-stub:+")
    implementation(project(":interceptor-http2-test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.28.2")
    testImplementation("io.grpc:grpc-testing:+")
    testImplementation("io.grpc:grpc-examples:+")
}

protobuf {
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:+"
        }
    }
    protoc {
        artifact = "com.google.protobuf:protoc:3.11.0"
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

tasks {
    val nativeAgentLinkTask = getByPath(":interceptor-http2-test:linkHookDebugSharedNativeAgent")
    val test by getting(Test::class) {
        testLogging.showStandardStreams = false
        doFirst {
            val extensions = setOf("so", "dylib", "dll")
            val nativeAgentLib = nativeAgentLinkTask.outputs.files.first().walkTopDown().last { it.extension in extensions }
            jvmArgs("-agentpath:$nativeAgentLib")
        }
    }
    test.dependsOn(nativeAgentLinkTask)
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
