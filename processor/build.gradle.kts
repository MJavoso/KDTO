import org.gradle.kotlin.dsl.provideDelegate

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("maven-publish")
    `kotlin-dsl`
}

group = "io.github.mjavoso"
version = "1.0.0-alpha01"

val pluginVersion: String = version.toString()
val pluginGroup: String = group.toString()

dependencies {
    implementation(libs.ksp.symbol.processor)
    implementation(libs.kotlin.poet)
    implementation(project(":annotations"))
    implementation(localGroovy())
    implementation(gradleApi())
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("kdtoPlugin") {
            id = "io.github.mjavoso.kdto.plugin"
            version = pluginVersion
            implementationClass = "com.marcode.kdto.plugin.KDTOGradlePlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            group = pluginGroup
            artifactId = "kdto-processor"
            version = pluginVersion
        }
    }
    repositories {
        mavenLocal()
    }
}