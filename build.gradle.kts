plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.jreleaser) apply false
}

group = "com.marcode"
version = "1.0.0-alpha01"

repositories {
    mavenCentral()
    mavenLocal()
}
