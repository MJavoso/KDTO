package com.marcode.kdto.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KDTOGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        target.dependencies {
            kdto()
        }

        target.registerTasks()
    }

    private fun Project.registerTasks() {
        afterEvaluate {
            val kspTask = tasks.findByName("kspKotlin") ?: run {
                logger.warn("No 'kspKotlin' task found")
                return@afterEvaluate
            }
            tasks.register("generateKDto") {
                group = "kdto"
                dependsOn(kspTask)
            }
        }
    }
}