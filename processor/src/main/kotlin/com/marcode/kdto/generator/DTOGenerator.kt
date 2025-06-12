package com.marcode.kdto.generator

import com.google.devtools.ksp.processing.KSPLogger
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

        fileSpec.addImport(dto.originalPackageName, dto.originalClassName)

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
        this@resolveImports.includedProperties.forEach { property ->
            val type = property.type.resolve()
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
        this.includedProperties.forEach { property ->
            val name = property.simpleName.asString()
            val typeName = property.type.resolve().toTypeName()
            val paramSpec = ParameterSpec.builder(name, typeName)
                .build()
            val propertySpec = PropertySpec.builder(name, typeName, KModifier.PUBLIC)
                .initializer(name)
                .addAnnotations(annotateClassProperty(property))
                .build()
            constructorSpec.addParameter(paramSpec)
            classSpec.addProperty(propertySpec)
        }
    }

    private fun annotateClassProperty(property: KSPropertyDeclaration): List<AnnotationSpec> {
        return property.annotations.map { annotation ->
            val annotationDecl = annotation.annotationType.resolve().declaration
            val annotationClass = ClassName(annotationDecl.packageName.asString(), annotationDecl.simpleName.asString())
            val annotationSpec = AnnotationSpec.builder(annotationClass)
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
        annotations.forEach { annotation ->
            val annotationDeclaration = annotation.annotationType.resolve().declaration
            val annotationClassName = ClassName(annotationDeclaration.packageName.asString(), annotationDeclaration.simpleName.asString())
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

            fileSpec.addImport(annotationDeclaration.packageName.asString(), annotationDeclaration.simpleName.asString())
            classSpec.addAnnotation(annotationSpec.build())
        }
    }

    private fun DtoDeclaration.createMappperExtension(packageName: String): FunSpec {
        val dtoType = ClassName(packageName, dtoName)
        return FunSpec.builder("to$dtoName")
            .receiver(ClassName(originalPackageName, originalClassName))
            .returns(dtoType)
            .addCode(buildCodeBlock {
                add("return %T(\n", dtoType)
                includedProperties.forEach { property ->
                    val propertyName = property.simpleName.asString()
                    add("\t%L = this.%L,\n", propertyName, propertyName)
                }
                add(")\n")
            })
            .build()
    }
}