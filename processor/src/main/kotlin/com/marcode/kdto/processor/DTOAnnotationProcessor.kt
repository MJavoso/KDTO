package com.marcode.kdto.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.marcode.kdto.annotations.Dto
import com.marcode.kdto.annotations.DtoSpec
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.processor.util.getArgument
import kotlin.collections.emptyList

internal class DTOAnnotationProcessor(
    private val classDeclaration: KSClassDeclaration,
    private val logger: KSPLogger
) {
    val dtoDeclarations: List<DtoDeclaration>

    init {
        dtoDeclarations = processDtoAnnotations()
    }

    private fun processDtoAnnotations(): List<DtoDeclaration> {
        val dtoAnnotation = classDeclaration.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString()!! == Dto::class.qualifiedName }
            ?: run {
                return emptyList()
            }
        val dtoSpecs = dtoAnnotation.arguments[0].toDtoSpec()

        val constructorParams = classDeclaration.primaryConstructor!!.parameters.mapNotNull { it.name?.asString() }.toSet()
        val classProperties = classDeclaration.getDeclaredProperties()
            .filter { it.simpleName.asString() in constructorParams }
        val packageName = classDeclaration.packageName.asString()

        return dtoSpecs.map { dtoSpec ->
            logger.info("Class properties ${classDeclaration.simpleName.asString()}: ${classProperties.map { it.simpleName.asString() }.toList()}")
            logger.logging("Including properties: ${dtoSpec.include.contentToString()}")
            logger.logging("Excluding properties: ${dtoSpec.exclude.contentToString()}")
            val properties = when {
                dtoSpec.include.isNotEmpty() -> classProperties.filter { it.simpleName.asString() in dtoSpec.include }
                dtoSpec.exclude.isNotEmpty() -> classProperties.filterNot { it.simpleName.asString() in dtoSpec.exclude }
                else -> classProperties
            }
            logger.info("Transfered properties: ${properties.map { it.simpleName.asString() }.toList()}")
            return@map DtoDeclaration(
                packageName = packageName,
                originalClassName = classDeclaration.simpleName.asString(),
                originalPackageName = classDeclaration.packageName.asString(),
                dtoName = dtoSpec.dtoName,
                includedProperties = properties.toList()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KSValueArgument.toDtoSpec(): List<DtoSpec> {
        val specs = value as? List<*> ?: return emptyList()
        return specs.mapNotNull { spec ->
            val annotation = spec as? KSAnnotation ?: return@mapNotNull null
            val name = annotation.getArgument(0) ?: ""
            val include = annotation.getArgument<List<String>>("include")?.toTypedArray() ?: emptyArray()
            val exclude = annotation.getArgument<List<String>>("exclude")?.toTypedArray() ?: emptyArray()
            DtoSpec(name, include, exclude)
        }
    }
}