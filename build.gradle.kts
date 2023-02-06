plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl-base`.apply(false)
    `kotlin-dsl`.apply(false)
    kotlin("jvm").apply(false)
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").apply(false)
    id("io.github.gradle-nexus.publish-plugin")
    id("com.github.hierynomus.license").apply(false)
    id("com.google.protobuf").apply(false)
    id("com.epam.drill.gradle.plugin.kni").apply(false)
}

group = "com.epam.drill"

repositories {
    mavenLocal()
    mavenCentral()
}
