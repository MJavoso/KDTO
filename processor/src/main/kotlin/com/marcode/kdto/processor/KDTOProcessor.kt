package com.marcode.kdto.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.marcode.kdto.annotations.Dto
import com.marcode.kdto.annotations.definitions.DtoDef
import com.marcode.kdto.annotations.definitions.DtoProperty
import com.marcode.kdto.generator.DTOGenerator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.OutputStreamWriter

class KDTOProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
): SymbolProcessor {
    private val filteredAnnotations = listOf(Dto::class, DtoDef::class, DtoProperty::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        processDtos(resolver)
        processDtoDefinitions(resolver)

        return emptyList()
    }

    private fun processDtos(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation(Dto::class.qualifiedName!!)
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDecl ->
            val dtoProcessor = DTOAnnotationProcessor(classDecl, logger)
            val generator = DTOGenerator(dtoProcessor.dtoDeclarations, logger, filterAnnotations = filteredAnnotations)
            generator.dtoFiles.forEach { fileSpec ->
                val file = codeGenerator.createNewFile(
                    dependencies = Dependencies(false, classDecl.containingFile!!),
                    packageName = fileSpec.packageName,
                    fileName = fileSpec.name
                )

                OutputStreamWriter(file, Charsets.UTF_8).use { writer ->
                    fileSpec.writeTo(writer)
                    logger.info("Generated DTO: ${fileSpec.name} from ${classDecl.simpleName.asString()}")
                }
            }
        }
    }

    private fun processDtoDefinitions(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation(DtoDef::class.qualifiedName!!)
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDecl ->
            val dtoDefinitonProcessor = DTODefinitionProcessor(classDecl, logger)
            val generator = DTOGenerator(listOf(dtoDefinitonProcessor.dtoDefinition), logger, filterAnnotations = filteredAnnotations)
            generator.dtoFiles.forEach { fileSpec ->
                val file = codeGenerator.createNewFile(
                    dependencies = Dependencies(false, classDecl.containingFile!!),
                    packageName = fileSpec.packageName,
                    fileName = fileSpec.name
                )

                OutputStreamWriter(file, Charsets.UTF_8).use { writer ->
                    fileSpec.writeTo(writer)
                    logger.info("Generated DTO: ${fileSpec.name} from ${classDecl.simpleName.asString()}")
                }
            }
        }
    }
}