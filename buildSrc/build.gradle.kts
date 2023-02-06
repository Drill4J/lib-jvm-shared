plugins {
    `java-gradle-plugin`
    kotlin("jvm").apply(false)
    kotlin("multiplatform").apply(false)
    id("com.github.hierynomus.license").apply(false)
}

group = "com.epam.drill"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":kni-plugin"))
}
