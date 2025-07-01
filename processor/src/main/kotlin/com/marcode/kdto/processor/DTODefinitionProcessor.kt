package com.marcode.kdto.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.marcode.kdto.annotations.Dto
import com.marcode.kdto.annotations.definitions.DtoDef
import com.marcode.kdto.annotations.exceptions.DtoDefinitionConflictException
import com.marcode.kdto.annotations.exceptions.PropertyNotFoundException
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.processor.data.DtoDefAnnotationDto
import com.marcode.kdto.processor.data.DtoProperty
import com.marcode.kdto.util.getArgument
import com.marcode.kdto.util.isOfType
import com.marcode.kdto.annotations.definitions.DtoProperty as DtoPropertyAnnotation

internal class DTODefinitionProcessor(
    private val classDeclaration: KSClassDeclaration,
    private val logger: KSPLogger
) {
    val dtoDefinition = processDtoDefAnnotations()

    private data class DtoPropertyAnnotationDto(
        val from: String,
        val property: KSPropertyDeclaration,
        val isPropertyFromSourceClass: Boolean,
        val includeSourceAnnotations: Boolean = true,
        val sourceAnnotations: List<KSAnnotation> = emptyList()
    ) {
        val sourceName = if (from.isNotBlank()) from else property.simpleName.asString()
    }

    private fun processDtoDefAnnotations(): DtoDeclaration {
        val classAnnotations = classDeclaration.annotations
        val dtoDefAnnotation = classAnnotations.firstOrNull { it.isOfType(DtoDef::class) }
            ?.toDtoDef()
            ?: run {
                throw RuntimeException("Unknown error for ${classDeclaration.qualifiedName?.asString() ?: classDeclaration.simpleName.asString()}")
            }
        if (dtoDefAnnotation.dtoName.isBlank() && classDeclaration.getVisibility() != Visibility.PRIVATE) {
            throw DtoDefinitionConflictException("Class ${classDeclaration.qualifiedName?.asString()} is not private and no explicit dto name was provided.")
        }
        // Block for getting properties on the source class and apply configuration of DtoDef
        val sourceClass = dtoDefAnnotation.sourceClass.declaration as KSClassDeclaration
        val sourceClassConstructorParams =
            sourceClass.primaryConstructor!!.parameters.mapNotNull { it.name?.asString() }.toSet()
        val sourceClassProperties = sourceClass.getDeclaredProperties()
            .filter { it.simpleName.asString() in sourceClassConstructorParams }

        val includedSourceProperties = getIncludedPropertiesFromSourceClass(dtoDefAnnotation, sourceClassProperties)
        val sourceAnnotations = if (dtoDefAnnotation.includeSourceAnnotations) {
            sourceClass.annotations.filterNot {
                it.isOfType(Dto::class) // Source class may also use regular Dto annotation.
            }.toList()
        } else emptyList()

        // Block for specific property details on interface class and apply configuration of DtoProperty
        val (annotatedProperties, regularProperties) = classDeclaration.getDeclaredProperties()
            .partition { property ->
                property.annotations.any { annotation -> annotation.isOfType(DtoProperty::class) }
            }

        validateRegularProperties(regularProperties, dtoDefAnnotation, includedSourceProperties, sourceClass)

        val dtoPropertyAnnotations = getDtoPropertyAnnotations(annotatedProperties, includedSourceProperties, sourceClass, dtoDefAnnotation)

        logger.logging("Regular properties: ${regularProperties.map { it.simpleName.asString() }}")
        logger.logging("DtoProperty annotations: ${dtoPropertyAnnotations.joinToString { "${it.property.simpleName.asString()} (${it.from})" }}")
        logger.logging(
            "Included source properties: ${
                includedSourceProperties.map { it.simpleName.asString() }.toList()
            }"
        )

        logger.logging("Include source annotations: ${dtoDefAnnotation.includePropertySourceAnnotations}")

        val includedProperties =
            getIncludedProperties(regularProperties, includedSourceProperties, dtoDefAnnotation, dtoPropertyAnnotations)

        logger.logging("Included properties: ${includedProperties.joinToString { "${it.property.simpleName.asString()} (${it.from})" }}")

        return DtoDeclaration(
            packageName = sourceClass.packageName.asString(),
            originalClassName = sourceClass.simpleName.asString(),
            dtoName = dtoDefAnnotation.dtoName.takeIf { it.isNotBlank() } ?: classDeclaration.simpleName.asString(),
            includedProperties = includedProperties,
            classAnnotations = sourceAnnotations
        )
    }

    private fun getIncludedProperties(
        regularProperties: List<KSPropertyDeclaration>,
        includedSourceProperties: Sequence<KSPropertyDeclaration>,
        dtoDefAnnotation: DtoDefAnnotationDto,
        dtoPropertyAnnotations: List<DtoPropertyAnnotationDto>
    ): List<DtoProperty> = regularProperties
        .map { property ->
            val sourceProperty = includedSourceProperties.firstOrNull { it.simpleName.asString() == property.simpleName.asString() }
            DtoProperty(
                property,
                propertyExistsInSourceClass = sourceProperty != null,
                includeSourceAnnotations = dtoDefAnnotation.includePropertySourceAnnotations,
                sourceAnnotations = if (sourceProperty != null && dtoDefAnnotation.includePropertySourceAnnotations) sourceProperty.annotations.toList() else emptyList()
            )
        } + dtoPropertyAnnotations.map { dtoPropertyAnnotation ->
        DtoProperty(
            dtoPropertyAnnotation.property,
            from = dtoPropertyAnnotation.from,
            propertyExistsInSourceClass = dtoPropertyAnnotation.isPropertyFromSourceClass,
            includeSourceAnnotations = dtoPropertyAnnotation.includeSourceAnnotations,
            sourceAnnotations = dtoPropertyAnnotation.sourceAnnotations
        )
    } + includedSourceProperties
        .filterNot { sourceProperty ->
            dtoPropertyAnnotations.any { dto ->
                dto.sourceName == sourceProperty.simpleName.asString()
            } || regularProperties.any { it.simpleName.asString() == sourceProperty.simpleName.asString() }
        }
        .map { property ->
            DtoProperty(
                property,
                includeSourceAnnotations = dtoDefAnnotation.includePropertySourceAnnotations,
                propertyExistsInSourceClass = true,
                isSourceClassProperty = true
            )
        }

    private fun getDtoPropertyAnnotations(
        annotatedProperties: List<KSPropertyDeclaration>,
        includedSourceProperties: Sequence<KSPropertyDeclaration>,
        sourceClass: KSClassDeclaration,
        dtoDefAnnotation: DtoDefAnnotationDto
    ): List<DtoPropertyAnnotationDto> = annotatedProperties.mapNotNull { property ->
        property to (property.annotations.firstOrNull { annotation -> annotation.isOfType(DtoProperty::class) }
            ?.toDtoProperty() ?: return@mapNotNull null)
    }.map { (property, dtoPropertyAnnotation) ->
        // If "from" field is provided, it must exist in the source class
        logger.logging("Processing DtoProperty ${property.simpleName.asString()}")
        if (dtoPropertyAnnotation.from.isNotBlank()) {
            if (!includedSourceProperties.any { it.simpleName.asString() == dtoPropertyAnnotation.from }) {
                throw PropertyNotFoundException(
                    "Property ${property.simpleName.asString()} in class ${classDeclaration.qualifiedName?.asString()} " +
                            "references non-existent source property '${dtoPropertyAnnotation.from}' in class ${sourceClass.qualifiedName?.asString()}"
                )
            }
        }
        // Name of the property in the source class
        val sourceFrom = dtoPropertyAnnotation.from.takeIf { it.isNotBlank() }
            ?: property.simpleName.asString()
        logger.logging("Source property name: $sourceFrom")
        if (sourceFrom in dtoDefAnnotation.exclude) {
            throw DtoDefinitionConflictException("Class ${classDeclaration.qualifiedName?.asString()} references excluded property ${dtoPropertyAnnotation.from}. Remove it from the interface or from the excluded list.")
        }
        logger.logging("Property is not in excluded list. Verifying if it exists in source class as \"$sourceFrom\"")
        if (includedSourceProperties.firstOrNull { it.simpleName.asString() == sourceFrom } == null) {
            return@map DtoPropertyAnnotationDto(
                from = sourceFrom,
                isPropertyFromSourceClass = false,
                property = property
            )
        }
        logger.logging("Property is in source class")
        // If property comes from source class, it must match the source's type
        val propertyType = property.type.resolve().declaration
        val sourceProperty = includedSourceProperties.first {
            it.simpleName.asString() == sourceFrom
        }
        val sourceType = sourceProperty.type.resolve().declaration

        if ((propertyType.qualifiedName?.asString()
                ?: propertyType.simpleName.asString()) != (sourceType.qualifiedName?.asString()
                ?: sourceType.simpleName.asString())
        ) {
            throw DtoDefinitionConflictException(
                "Property ${property.simpleName.asString()} in class ${classDeclaration.qualifiedName?.asString()} " +
                        "has different type than source property '$sourceFrom' in class ${sourceClass.qualifiedName?.asString()}"
            )
        }

        logger.logging(
            "Source annotations of property ${property.simpleName.asString()}: ${
                sourceProperty.annotations.toList().joinToString { it.shortName.asString() }
            }"
        )
        logger.logging("Include source annotations for this property: ${dtoPropertyAnnotation.includeSourceAnnotations}")
        DtoPropertyAnnotationDto(
            from = sourceFrom,
            property = property,
            isPropertyFromSourceClass = true,
            includeSourceAnnotations = dtoPropertyAnnotation.includeSourceAnnotations,
            sourceAnnotations = if (dtoPropertyAnnotation.includeSourceAnnotations) {
                sourceProperty.annotations.toList()
            } else emptyList()
        )
    }

    private fun validateRegularProperties(
        regularProperties: List<KSPropertyDeclaration>,
        dtoDefAnnotation: DtoDefAnnotationDto,
        includedSourceProperties: Sequence<KSPropertyDeclaration>,
        sourceClass: KSClassDeclaration
    ) {
        if (regularProperties.any { it.simpleName.asString() in dtoDefAnnotation.exclude }) {
            throw DtoDefinitionConflictException("Class ${classDeclaration.qualifiedName?.asString()} declares excluded properties [${dtoDefAnnotation.exclude.joinToString()}]. Remove them from the interface or from the excluded list.")
        }

        if (regularProperties.any { property ->
                val sourceProperty =
                    includedSourceProperties.firstOrNull { it.simpleName.asString() == property.simpleName.asString() }
                        ?: return@any false
                property.type.resolve() != sourceProperty.type.resolve()
            }) {
            throw DtoDefinitionConflictException("Class ${classDeclaration.qualifiedName?.asString()} declares properties with different types than the ones in the source class ${sourceClass.qualifiedName?.asString()}.")
        }
    }

    private fun getIncludedPropertiesFromSourceClass(
        dtoSpec: DtoDefAnnotationDto,
        classProperties: Sequence<KSPropertyDeclaration>
    ): Sequence<KSPropertyDeclaration> = when {
        dtoSpec.include.isNotEmpty() -> {
            val classPropertyNames = classProperties.map { it.simpleName.asString() }.toSet()
            val includeSet = dtoSpec.include.toSet()
            val nonExistentProperties = includeSet - classPropertyNames
            if (nonExistentProperties.isNotEmpty()) {
                throw PropertyNotFoundException("Properties [${nonExistentProperties.joinToString()}] do not exist in class ${classDeclaration.packageName.asString()}")
            }
            classProperties.filter { it.simpleName.asString() in dtoSpec.include }
        }

        dtoSpec.exclude.isNotEmpty() -> {
            val classPropertyNames = classProperties.map { it.simpleName.asString() }.toSet()
            val excludeSet = dtoSpec.exclude.toSet()
            val nonExistentProperties = excludeSet - classPropertyNames
            if (nonExistentProperties.isNotEmpty()) {
                throw PropertyNotFoundException("Properties [${nonExistentProperties.joinToString()}] do not exist in class ${classDeclaration.packageName.asString()}")
            }

            classProperties.filterNot { it.simpleName.asString() in dtoSpec.exclude }
        }

        else -> classProperties
    }

    private fun KSAnnotation.toDtoDef(): DtoDefAnnotationDto {
        val sourceClass = getArgument<KSType>("sourceClass")!!
        val dtoName = getArgument<String>("dtoName") ?: ""
        val include = getArgument<List<String>>("include") ?: emptyList()
        val exclude = getArgument<List<String>>("exclude") ?: emptyList()
        val includeSourceAnnotations = getArgument<Boolean>("includeSourceAnnotations") ?: true
        val includePropertySourceAnnotations = getArgument<Boolean>("includePropertySourceAnnotations") ?: true

        return DtoDefAnnotationDto(
            sourceClass = sourceClass,
            dtoName = dtoName,
            include = include,
            exclude = exclude,
            includeSourceAnnotations = includeSourceAnnotations,
            includePropertySourceAnnotations = includePropertySourceAnnotations
        )
    }

    private fun KSAnnotation.toDtoProperty(): DtoPropertyAnnotation {
        return DtoPropertyAnnotation(
            from = this.getArgument<String>("from") ?: "",
            includeSourceAnnotations = this.getArgument("includeSourceAnnotations") ?: true
        )
    }
}