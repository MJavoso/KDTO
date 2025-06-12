plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.gradle.plugin.publish)
}

group = "io.github.mjavoso"
version = "1.0.0-alpha02"

dependencies {
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
    website = "https://github.com/MJavoso/KDTO"
    vcsUrl = "https://github.com/MJavoso/KDTO.git"
    plugins {
        create("kdtoPlugin") {
            id = "io.github.mjavoso.kdto.plugin"
            displayName = "Plugin for the KDTO generator"
            description = "Plugin that helps you configure automatically all the dependencies for KDTO class generator"
            tags = listOf("kotlin", "kdto")
            implementationClass = "com.marcode.kdto.plugin.KDTOGradlePlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "kdto-plugin"
            version = version.toString()
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "localPluginRepository"
            url = uri(layout.buildDirectory.dir("local-plugin-repository"))
        }
    }
}

signing {
    sign(publishing.publications)
    useGpgCmd()
}

tasks.register("listPublications") {
    doLast {
        println("Publications: ${publishing.publications.names}")
    }
}