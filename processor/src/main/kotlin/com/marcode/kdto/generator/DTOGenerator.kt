package com.marcode.kdto.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.util.getFormattedValue
import com.marcode.kdto.util.toTypeName
import com.squareup.kotlinpoet.*

internal class DTOGenerator(
    private val dtoDeclarations: List<DtoDeclaration>,
    private val logger: KSPLogger
) {
    val dtoFiles: List<FileSpec>

    init {
        dtoFiles = buildList {
            dtoDeclarations.forEach { dtoDeclaration ->
                add(generateDto(dtoDeclaration))
            }
        }
    }

    private fun generateDto(dto: DtoDeclaration): FileSpec {
        val fileSpec = FileSpec.builder("${dto.packageName}.generated", dto.dtoName)

        dto.resolveImports()
            .forEach { (packageName, className) ->
                fileSpec.addImport(packageName, className)
            }

        fileSpec.addImport(dto.packageName, dto.originalClassName)

        val classSpec = TypeSpec.classBuilder(dto.dtoName)
            .addModifiers(KModifier.DATA)
        val constructorSpec = FunSpec.constructorBuilder()

        dto.addClassProperties(classSpec, constructorSpec)

        classSpec.primaryConstructor(constructorSpec.build())
        dto.addAnnotations(fileSpec, classSpec)
        fileSpec.addType(classSpec.build())

        fileSpec.addFunction(dto.createMappperExtension(fileSpec.packageName))

        return fileSpec.build()
    }

    private fun DtoDeclaration.resolveImports() = buildList {
        this@resolveImports.includedProperties.forEach { dtoProperty ->
            val type = dtoProperty.property.type.resolve()
            val typeDeclaration = type.declaration
            val packageName = typeDeclaration.packageName.asString()
            val simpleName = typeDeclaration.simpleName.asString()
            if (packageName == "kotlin") {
                return@forEach
            }
            add(packageName to simpleName)
        }
    }

    private fun DtoDeclaration.addClassProperties(classSpec: TypeSpec.Builder, constructorSpec: FunSpec.Builder) {
        this.includedProperties.forEach { dtoProperty ->
            val name = dtoProperty.property.simpleName.asString()
            val typeName = dtoProperty.property.type.resolve().toTypeName()
            val paramSpec = ParameterSpec.builder(name, typeName)
                .build()
            val propertySpec = PropertySpec.builder(name, typeName, KModifier.PUBLIC)
                .initializer(name)
            if (dtoProperty.isSourceClassProperty && !dtoProperty.includeSourceAnnotations) {
                return@forEach
            }
            propertySpec.addAnnotations(annotateClassProperty(dtoProperty.property, dtoProperty.includeSourceAnnotations, dtoProperty.sourceAnnotations))
            constructorSpec.addParameter(paramSpec)
            classSpec.addProperty(propertySpec.build())
        }
    }

    private fun annotateClassProperty(
        property: KSPropertyDeclaration,
        includeSourceAnnotations: Boolean,
        sourceAnnotations: List<KSAnnotation> = emptyList(),
    ): List<AnnotationSpec> {
        val annotations = if (includeSourceAnnotations) {
            property.annotations.toList() + sourceAnnotations
        } else sourceAnnotations
        return annotations.map { annotation ->
            val annotationDecl = annotation.annotationType.resolve().declaration
            val annotationClass = ClassName(annotationDecl.packageName.asString(), annotationDecl.simpleName.asString())
            val annotationSpec = AnnotationSpec.builder(annotationClass)

            annotation.useSiteTarget?.let { useSite ->
                when (useSite) {
                    AnnotationUseSiteTarget.FIELD -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                    AnnotationUseSiteTarget.PROPERTY -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.PROPERTY)
                    AnnotationUseSiteTarget.GET -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    AnnotationUseSiteTarget.SET -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.SET)
                    AnnotationUseSiteTarget.RECEIVER -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.RECEIVER)
                    AnnotationUseSiteTarget.SETPARAM -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.SETPARAM)
                    AnnotationUseSiteTarget.FILE -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    AnnotationUseSiteTarget.PARAM -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.PARAM)
                    AnnotationUseSiteTarget.DELEGATE -> annotationSpec.useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE)
                    AnnotationUseSiteTarget.ALL -> Unit
                }
            }

            annotation.arguments.forEach { argument ->
                val name = argument.name
                val value = argument.getFormattedValue()
                if (name != null) {
                    annotationSpec.addMember("%L = %L", name.asString(), value)
                } else {
                    annotationSpec.addMember("%L", value)
                }
            }
            annotationSpec.build()
        }.toList()
    }

    private fun DtoDeclaration.addAnnotations(
        fileSpec: FileSpec.Builder,
        classSpec: TypeSpec.Builder,
    ) {
        classAnnotations.forEach { annotation ->
            val annotationDeclaration = annotation.annotationType.resolve().declaration
            val annotationClassName =
                ClassName(annotationDeclaration.packageName.asString(), annotationDeclaration.simpleName.asString())
            val annotationSpec = AnnotationSpec.builder(annotationClassName)

            annotation.arguments.forEach { annotationArgument ->
                val argName = annotationArgument.name
                val argValue = annotationArgument.getFormattedValue()
                if (argName != null) {
                    annotationSpec.addMember("%L = %L", argName.asString(), argValue)
                } else {
                    annotationSpec.addMember("%L", argValue)
                }
            }

            fileSpec.addImport(
                annotationDeclaration.packageName.asString(),
                annotationDeclaration.simpleName.asString()
            )
            classSpec.addAnnotation(annotationSpec.build())
        }
    }

    private fun DtoDeclaration.createMappperExtension(packageName: String): FunSpec {
        val dtoType = ClassName(packageName, dtoName)
        return FunSpec.builder("to$dtoName")
            .receiver(ClassName(packageName, originalClassName))
            .apply {
                includedProperties.filter { !it.propertyExistsInSourceClass }.forEach { dtoProperty ->
                    val propertyName = dtoProperty.property.simpleName.asString()
                    addParameter(propertyName, dtoProperty.property.type.resolve().toTypeName())
                }
            }
            .returns(dtoType)
            .addCode(buildCodeBlock {
                add("return %T(\n", dtoType)
                includedProperties.forEach { dtoProperty ->
                    val propertyName = dtoProperty.property.simpleName.asString()
                    if (dtoProperty.from.isNotBlank()) {
                        add("\t%L = this.%L\n", propertyName, dtoProperty.from)
                    } else if (!dtoProperty.propertyExistsInSourceClass) {
                        add("\t%L = %L,\n", propertyName, propertyName)
                    } else {
                        add("\t%L = this.%L,\n", propertyName, propertyName)
                    }
                }
                add(")\n")
            })
            .build()
    }
}