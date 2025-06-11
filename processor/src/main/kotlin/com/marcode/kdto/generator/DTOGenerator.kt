package com.marcode.kdto.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.util.getFormattedValue
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

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
        dto.includedProperties.forEach { property ->
            val name = property.simpleName.asString()
            val typeName = property.type.resolve().toTypeName()
            val paramSpec = ParameterSpec.builder(name, typeName)
                .build()
            val propertySpec = PropertySpec.builder(name, typeName, KModifier.PUBLIC)
                .initializer(name)
                .build()
            constructorSpec.addParameter(paramSpec)
            classSpec.addProperty(propertySpec)
        }
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

            println("Adding annotation: ${annotationSpec}")
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

    private fun KSType.toTypeName(): TypeName {
        val decl = this.declaration
        val pkg = decl.packageName.asString()
        val simpleName = decl.simpleName.asString()

        val typeArguments = this.arguments.map { arg ->
            arg.type?.resolve()?.toTypeName() ?: STAR
        }

        val className = ClassName(pkg, simpleName)
        val typeName = if (typeArguments.isNotEmpty()) {
            className.parameterizedBy(typeArguments)
        } else {
            className
        }

        return if (this.nullability == Nullability.NULLABLE) {
            typeName.copy(nullable = true)
        } else {
            typeName
        }
    }
}