package com.marcode.kdto.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.marcode.kdto.annotations.Dto
import com.marcode.kdto.annotations.DtoSpec
import com.marcode.kdto.annotations.exceptions.PropertyNotFoundException
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.processor.data.DtoProperty
import com.marcode.kdto.util.getArgument
import com.marcode.kdto.util.isOfType

internal class DTOAnnotationProcessor(
    private val classDeclaration: KSClassDeclaration,
    private val logger: KSPLogger
) {
    val dtoDeclarations = processDtoAnnotations()

    private fun processDtoAnnotations(): List<DtoDeclaration> {
        val classAnnotations = classDeclaration.annotations
        val dtoAnnotation = classAnnotations.firstOrNull { it.isOfType(Dto::class) }
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
            val properties = getIncludedProperties(dtoSpec, classProperties)
            val annotations = if (dtoSpec.includeAnnotations) {
                classAnnotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() != Dto::class.qualifiedName }.toList()
            } else emptyList()
            logger.info("Transfered properties: ${properties.map { it.simpleName.asString() }.toList()}")
            return@map DtoDeclaration(
                packageName = packageName,
                originalClassName = classDeclaration.simpleName.asString(),
                dtoName = dtoSpec.dtoName,
                includedProperties = properties.map { property ->
                    DtoProperty(property, propertyExistsInSourceClass = true, includeSourceAnnotations = dtoSpec.includeAnnotations)
                }.toList(),
                classAnnotations = annotations
            )
        }
    }

    private fun getIncludedProperties(
        dtoSpec: DtoSpec,
        classProperties: Sequence<KSPropertyDeclaration>
    ): Sequence<KSPropertyDeclaration> = when {
        dtoSpec.include.isNotEmpty() -> {
            val classPropertyNames = classProperties.map { it.simpleName.asString() }.toSet()
            val includeSet = dtoSpec.include.toSet()
            val nonExistentProperties = includeSet - classPropertyNames
            if (nonExistentProperties.isNotEmpty()) {
                throw PropertyNotFoundException("Properties [${nonExistentProperties.joinToString()}] do not exist in class ${classDeclaration.qualifiedName?.asString()}")
            }
            classProperties.filter { it.simpleName.asString() in dtoSpec.include }
        }

        dtoSpec.exclude.isNotEmpty() -> {
            val classPropertyNames = classProperties.map { it.simpleName.asString() }.toSet()
            val excludeSet = dtoSpec.exclude.toSet()
            val nonExistentProperties = excludeSet - classPropertyNames
            if (nonExistentProperties.isNotEmpty()) {
                throw PropertyNotFoundException("Properties [${nonExistentProperties.joinToString()}] do not exist in class ${classDeclaration.qualifiedName?.asString()}")
            }

            classProperties.filterNot { it.simpleName.asString() in dtoSpec.exclude }
        }

        else -> classProperties
    }

    @Suppress("UNCHECKED_CAST")
    private fun KSValueArgument.toDtoSpec(): List<DtoSpec> {
        val specs = value as? List<*> ?: return emptyList()
        return specs.mapNotNull { spec ->
            val annotation = spec as? KSAnnotation ?: return@mapNotNull null
            val name = annotation.getArgument<String>("dtoName") ?: ""
            val include = annotation.getArgument<List<String>>("include")?.toTypedArray() ?: emptyArray()
            val exclude = annotation.getArgument<List<String>>("exclude")?.toTypedArray() ?: emptyArray()
            val includeAnnotations = annotation.getArgument<Boolean>("includeAnnotations") ?: true
            DtoSpec(name, include, exclude, includeAnnotations)
        }
    }
}