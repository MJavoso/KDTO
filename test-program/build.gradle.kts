plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.ksp)
    id("io.github.mjavoso.kdto.plugin") version "1.0.0-alpha02"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}