package com.marcode.kdto.processor.data

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal data class DtoDeclaration(
    val packageName: String,
    val originalClassName: String,
    val originalPackageName: String,
    val dtoName: String,
    val includedProperties: List<DtoProperty>,
    val classAnnotations: List<KSAnnotation>
)
