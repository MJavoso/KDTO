plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.gradle.plugin.publish)
}

group = "io.github.mjavoso"
version = libs.versions.kdto.plugin.get()

dependencies {
    implementation(localGroovy())
    implementation(gradleApi())
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

tasks.register("generateDependenciesFile") {
    doLast {
        val annotationsVersion = libs.versions.kdto.annotations.get()
        val processorVersion = libs.versions.kdto.processor.get()

        val outputDir = layout.buildDirectory.dir("generated/kotlin").get().asFile
        outputDir.mkdirs()

        val dependenciesFile = File(outputDir, "com/marcode/kdto/plugin/Dependencies.kt")
        dependenciesFile.parentFile.mkdirs()

        dependenciesFile.writeText("""
            // AUTO-GENERATED FILE - DO NOT EDIT
            // Generated from libs.versions.toml
            package com.marcode.kdto.plugin
            
            import org.gradle.api.artifacts.dsl.DependencyHandler
            
            internal object Versions {
                const val kdtoProcessor = "$processorVersion"
                const val kdtoAnnotations = "$annotationsVersion"
            }
            
            internal object Dependencies {
                val annotations = "io.github.mjavoso:kdto-annotations:${'$'}{Versions.kdtoAnnotations}"
                val processor = "io.github.mjavoso:kdto-processor:${'$'}{Versions.kdtoProcessor}"
            }
            
            internal fun DependencyHandler.kdto() {
                implementation(Dependencies.annotations)
                ksp(Dependencies.processor)
            }
            
            private fun DependencyHandler.implementation(projectDependency: String) {
                add("implementation", projectDependency)
            }
            
            private fun DependencyHandler.ksp(projectDependency: String) {
                add("ksp", projectDependency)
            }
        """.trimIndent())

        println("✅ Generated Dependencies.kt with versions:")
        println("   - kdto-annotations: $annotationsVersion")
        println("   - kdto-processor: $processorVersion")
    }
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