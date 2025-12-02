package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSAnnotation

internal data class DtoDeclaration(
    val packageName: String,
    val originalClassName: String,
    val dtoName: String,
    val includedProperties: List<DtoProperty>,
    val annotationCollection: AnnotationCollection
)

internal sealed interface AnnotationCollection {
    data class DtoCollection(val classAnnotations: List<KSAnnotation>): AnnotationCollection
    data class DtoDefCollection(val sourceClassAnnotations: List<KSAnnotation>, val dtoDefinitionAnnotations: List<KSAnnotation>): AnnotationCollection
}