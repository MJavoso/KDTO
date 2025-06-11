package com.marcode.kdto.plugin

import org.gradle.api.artifacts.dsl.DependencyHandler

internal object Versions {
    val kdtoProcessor = "1.0.0-alpha02"
    val kdtoAnnotations = "1.0.0-alpha01"
}

internal object Dependencies {
    val annotations = "io.github.mjavoso:kdto-annotations:${Versions.kdtoAnnotations}"
    val processor = "io.github.mjavoso:kdto-processor:${Versions.kdtoProcessor}"
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