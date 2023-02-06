rootProject.name = "shared-libs"

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val publishVersion: String by extra
    val protobufVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("io.github.gradle-nexus.publish-plugin") version publishVersion
        id("com.google.protobuf") version protobufVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

include("kni-runtime")
include("kni-plugin")
include("jvmapi")
include("logger-api")
include("logger")
include("logger-test-agent")
include("common")
include("knasm")
include("drill-hook")
include("http-clients-instrumentation")
include("gradle-plugin")
include("transport")
include("interceptor-http")
include("interceptor-http-test")
include("interceptor-http2")
include("interceptor-http2-test")
include("interceptor-http2-test-grpc")
include("plugin-api-agent")
include("plugin-api-admin")
include("agent")
include("agent-runner-common")
include("agent-runner-gradle")
