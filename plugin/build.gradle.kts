plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `kotlin-dsl`
}

group = "io.github.mjavoso"
version = "1.0.0-alpha01"

dependencies {
    implementation(project(":processor"))
    implementation(localGroovy())
    implementation(gradleApi())
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("kdtoPlugin") {
            id = "io.github.mjavoso.kdto.plugin"
            implementationClass = "com.marcode.kdto.plugin.KDTOGradlePlugin"
        }
    }
}