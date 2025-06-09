import org.jreleaser.model.Active

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `maven-publish`
    id(libs.plugins.jreleaser.get().pluginId)
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

            pom {
                name.set("KDTO Annotations")
                description.set("Annotation definitions for the KDTO code generation processor")
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
                    url.set("https://github.com/MJavoso/KDTO")
                    connection.set("scm:git:https://github.com/MJavoso/KDTO.git")
                    developerConnection.set("scm:git:ssh://github.com/MJavoso/KDTO.git")
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