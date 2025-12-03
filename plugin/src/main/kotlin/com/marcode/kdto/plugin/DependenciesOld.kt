/*
package com.marcode.kdto.plugin

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler

internal object Dependencies {
    fun annotations(project: Project) = project.findVersionFromCatalog(
        alias = "kdto-annotations"
    )
    fun processor(project: Project) = project.findVersionFromCatalog(
        alias = "kdto-processor"
    )
}

private fun Project.findVersionFromCatalog(alias: String): String {
    return try {
        // Intenta obtener la versión del catálogo
        val versionCatalog = extensions.findByType(VersionCatalogsExtension::class.java)
        versionCatalog?.named("libs")?.findVersion(alias)?.get()?.toString()
            ?: throw IllegalStateException("Version $alias not found in catalog")
    } catch (e: Exception) {
        // Fallback por si no está disponible
        logger.warn("Could not find version for $alias in catalog")
        throw e
    }
}

internal fun DependencyHandler.kdto(project: Project) {
    implementation(Dependencies.annotations(project))
    ksp(Dependencies.processor(project))
}

private fun DependencyHandler.implementation(projectDependency: String) {
    add("implementation", projectDependency)
}

private fun DependencyHandler.ksp(projectDependency: String) {
    add("ksp", projectDependency)
}*/
