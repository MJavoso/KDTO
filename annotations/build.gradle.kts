plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `maven-publish`
}

group = "io.github.mjavoso"
version = "1.0.0-alpha01"

val pluginVersion: String = version.toString()
val pluginGroup: String = group.toString()

repositories {
    mavenCentral()
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            group = pluginGroup
            artifactId = "kdto-annotations"
            version = pluginVersion
        }
    }
    repositories {
        mavenLocal()
    }
}