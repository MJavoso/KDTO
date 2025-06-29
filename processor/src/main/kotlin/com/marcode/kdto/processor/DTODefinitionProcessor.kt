package com.marcode.kdto.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.marcode.kdto.annotations.definitions.DtoDef
import com.marcode.kdto.processor.data.DtoDeclaration
import com.marcode.kdto.processor.data.DtoDefAnnotation
import com.marcode.kdto.util.getArgument

internal class DTODefinitionProcessor(
    private val classDeclaration: KSClassDeclaration,
    private val logger: KSPLogger
) {
    val dtoDefinitions = processDtoDefAnnotations()

    private fun processDtoDefAnnotations(): List<DtoDeclaration> {
        val classAnnotations = classDeclaration.annotations
        val dtoDefAnnotation = classAnnotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString()!! == DtoDef::class.qualifiedName
        }?.toDtoDef()
            ?: run {
                return emptyList()
            }
        TODO("""
            - Obtener las propiedades de la clase fuente a través de su constructor primario.
            - Verificar si hay alguna propiedad existente de la clase fuente a través de include y exclude
            - Obtener sus anotaciones.
            - Incluir y excluir las propiedades según se requiera.
            - Copiar las anotaciones a nivel de clase si es necesario.
            - Copiar las anotaciones a nivel de propiedad si es necesario.
            - Analizar las propiedades de la interfaz.
            - Verificar si tiene la anotación @DtoProperty para extender la configuración.
            - Verificar si tiene la propiedad from. Si la tiene, verificar si existe en la clase fuente. Si no existe, lanzar error.
            - Verificar si se permite incluir las anotaciones de origen.
        """.trimIndent())
        return emptyList()
    }

    private fun KSAnnotation.toDtoDef(): DtoDefAnnotation {
        val sourceClass = getArgument<KSType>("sourceClass")!!
        val dtoName = getArgument<String>("dtoName") ?: ""
        val include = getArgument<List<String>>("include") ?: emptyList()
        val exclude = getArgument<List<String>>("exclude") ?: emptyList()
        val includeSourceAnnotations = getArgument<Boolean>("includeSourceAnnotations") ?: true
        val includePropertySourceAnnotations = getArgument<Boolean>("includePropertySourceAnnotations") ?: true

        return DtoDefAnnotation(
            sourceClass = sourceClass,
            dtoName = dtoName,
            include = include,
            exclude = exclude,
            includeSourceAnnotations = includeSourceAnnotations,
            includePropertySourceAnnotations = includePropertySourceAnnotations
        )
    }
}