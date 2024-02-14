rootProject.name = "lib-jvm-shared"

pluginManagement {
    val kotlinVersion: String by extra
    val kotlinxBenchmarkVersion: String by extra
    val atomicfuVersion: String by extra
    val licenseVersion: String by extra
    val publishVersion: String by extra
    val protobufVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("kotlinx-atomicfu") version atomicfuVersion
        id("org.jetbrains.kotlinx.benchmark") version kotlinxBenchmarkVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("io.github.gradle-nexus.publish-plugin") version publishVersion
        id("com.google.protobuf") version protobufVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy.eachPlugin {
        if(requested.id.id == "kotlinx-atomicfu") useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${target.version}")
    }
}

include("jvmapi")
include("logging-native")
include("logging")
include("common")
include("knasm")
include("interceptor-hook")
include("interceptor-http")
include("interceptor-stub")
include("plugin-api-admin")
include("agent-config")
include("agent-transport")
include("agent-instrumentation")
include("dsm")
include("dsm-annotations")
include("dsm-test-framework")
include("dsm-benchmarks")
include("ktor-swagger")
include("ktor-swagger-sample")
include("admin-analytics")
include("test2code-api")
include("test2code-common")
include("konform")
