import org.jreleaser.model.Active

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("maven-publish")
    id(libs.plugins.jreleaser.get().pluginId)
}

group = "io.github.mjavoso"
version = "1.0.0-beta01"

val pluginVersion: String = version.toString()
val pluginGroup: String = group.toString()

dependencies {
    implementation(libs.ksp.symbol.processor)
    implementation(libs.kotlin.poet)
    implementation(project(":annotations"))
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.compile.testing.ksp)
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            group = pluginGroup
            artifactId = "kdto-processor"
            version = pluginVersion

            pom {
                name.set("KDTO Processor")
                description.set("Generate multiple DTOs and mappers from a single Kotlin data class")
                url.set("https://github.com/MJavoso/KDTO")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("MJavoso")
                        name.set("Marco Pinedo")
                    }
                }
                scm {
                    url.set("https://github.com/mjavoso/kdto")
                    connection.set("scm:git:https://github.com/mjavoso/kdto.git")
                    developerConnection.set("scm:git:ssh://github.com/mjavoso/kdto.git")
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch = true
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}